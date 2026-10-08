package com.deepakkhatri.qa.pages;

import com.deepakkhatri.qa.config.Configuration;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Shared behaviour for every page object.
 *
 * <p>Two rules hold across the framework:
 *
 * <ul>
 *   <li><b>Explicit waits only.</b> There is no implicit wait configured
 *       anywhere. Mixing implicit and explicit waits produces unpredictable
 *       timeouts that are neither value, and {@code Thread.sleep} trades
 *       wall-clock time for reliability it does not actually buy.
 *   <li><b>Page objects never assert.</b> They expose intent and return state;
 *       the assertions live in the tests, so a failure names the business rule
 *       that broke rather than a helper several frames down the stack.
 * </ul>
 */
public abstract class BasePage {

    /** See {@link #clickUntil} for why a click may need repeating. */
    private static final int CLICK_ATTEMPTS = 3;
    private static final Duration CLICK_OUTCOME_TIMEOUT = Duration.ofSeconds(5);

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Configuration.explicitWait());
    }

    /** A locator present only once this page has finished rendering. */
    protected abstract By pageMarker();

    public boolean isDisplayed() {
        try {
            return waitForVisible(pageMarker()).isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void waitUntilReady() {
        waitForVisible(pageMarker());
    }

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Clicks an element.
     *
     * <p>Deliberately a plain native click with no scripted pre-check. Running
     * JavaScript immediately before a native click leaves Chrome's hit-test
     * data stale, and the click is then dispatched to nothing while WebDriver
     * reports success — measured, and the reason the scripted probe used for the
     * slide-out menu is confined to paths that also click via script.
     */
    protected void click(By locator) {
        waitForClickable(locator).click();
    }

    /**
     * Clicks, then confirms the click actually did something, retrying if not.
     *
     * <p>Headless Chrome on a loaded CI runner intermittently reports a
     * successful click that the page never receives — no navigation, no state
     * change, no exception. Measured on GitHub's runners: 13 of 33 tests failed
     * this way on Chrome while Firefox passed and the same commit was green
     * locally. The failures were always "the cart is empty" or "the badge did
     * not change" — never a wrong value, which is what distinguishes a lost
     * click from an application defect.
     *
     * <p>The retry is bounded and the post-condition is mandatory, so this
     * cannot quietly pass a broken build: if the outcome never arrives, the
     * test fails with a message naming both the control and the expectation.
     *
     * <p>It does trade away the ability to detect an application that needs two
     * clicks to register one action. That is a deliberate trade, and the
     * alternative is a suite nobody trusts.
     */
    protected void clickUntil(By locator, ExpectedCondition<?> outcome) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= CLICK_ATTEMPTS; attempt++) {
            waitForClickable(locator).click();

            try {
                new WebDriverWait(driver, CLICK_OUTCOME_TIMEOUT).until(outcome);
                return;
            } catch (TimeoutException e) {
                lastFailure = e;
            }
        }

        throw new IllegalStateException(
                "Clicked %s %d times but the expected outcome never happened: %s"
                        .formatted(locator, CLICK_ATTEMPTS, outcome),
                lastFailure);
    }

    protected void type(By locator, String text) {
        WebElement field = waitForVisible(locator);
        field.clear();
        field.sendKeys(text);
    }

    protected String textOf(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected List<WebElement> allElements(By locator) {
        return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
    }

    protected void selectByValue(By locator, String value) {
        new Select(waitForVisible(locator)).selectByValue(value);
    }

    /**
     * Presence check that does not wait.
     *
     * <p>Used where absence is the expected outcome — waiting the full timeout
     * for something that should not be there adds the timeout to every run for
     * no diagnostic gain.
     */
    protected boolean isPresent(By locator) {
        try {
            return !driver.findElements(locator).isEmpty();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    protected void waitUntilGone(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public String currentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Parses a rendered money string ("$29.99") into cents.
     *
     * <p>Cents rather than doubles: floating-point currency arithmetic makes
     * a correct checkout total fail an equality assertion.
     */
    protected static long parseMoneyToCents(String text) {
        String digits = text.replaceAll("[^0-9.]", "");

        if (digits.isBlank()) {
            throw new IllegalArgumentException("No money value found in: '" + text + "'");
        }

        return Math.round(Double.parseDouble(digits) * 100);
    }
}
