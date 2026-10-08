package com.deepakkhatri.qa.pages.components;

import com.deepakkhatri.qa.config.Configuration;
import com.deepakkhatri.qa.driver.Actionability;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The header is present on every signed-in page, so it is modelled as a
 * component the pages compose rather than duplicated into each page object.
 */
public class HeaderComponent {

    private static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");
    private static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By LOGOUT_LINK = By.cssSelector("[data-test='logout-sidebar-link']");
    private static final By RESET_LINK = By.cssSelector("[data-test='reset-sidebar-link']");

    /*
     * The application puts data-test="open-menu" / "close-menu" on the icon
     * <img>, which the real <button> overlays — clicking the documented test id
     * throws ElementClickInterceptedException. These two locators deliberately
     * target the buttons by id. Every other locator here uses a data-test hook.
     */
    private static final By MENU_BUTTON = By.id("react-burger-menu-btn");
    private static final By CLOSE_MENU_BUTTON = By.id("react-burger-cross-btn");

    /*
     * The slide-out menu is never removed from the DOM. Closed, it is merely
     * translated 300px off-screen, so its links still report display:block and
     * visibility:visible — ExpectedConditions.elementToBeClickable therefore
     * calls them clickable, and Selenium clicks a point outside the viewport.
     *
     * aria-hidden is not a sufficient signal either: the attribute flips to
     * "false" while the CSS transform is still animating. Measured mid-open,
     * the logout link sits at x = -276 with aria-hidden already "false", and
     * document.elementFromPoint at its centre returns null.
     *
     * Both conditions are therefore required: the attribute to know the menu is
     * opening, and Actionability.settledAndHittable to know it has arrived.
     */
    private static final By MENU_WRAPPER = By.cssSelector(".bm-menu-wrap");
    private static final String ARIA_HIDDEN = "aria-hidden";

    private static final int CLICK_ATTEMPTS = 3;
    private static final Duration CLICK_OUTCOME_TIMEOUT = Duration.ofSeconds(5);

    private final WebDriver driver;
    private final WebDriverWait wait;

    public HeaderComponent(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Configuration.explicitWait());
    }

    /** The badge is absent rather than zero when the cart is empty. */
    public int cartCount() {
        if (driver.findElements(CART_BADGE).isEmpty()) {
            return 0;
        }

        return Integer.parseInt(
                wait.until(ExpectedConditions.visibilityOfElementLocated(CART_BADGE))
                        .getText().trim());
    }

    public boolean isCartBadgeVisible() {
        return !driver.findElements(CART_BADGE).isEmpty();
    }

    /**
     * Opens the cart, confirming the navigation actually happened.
     *
     * <p>Same mitigation as {@code BasePage.clickUntil} — headless Chrome on a
     * loaded CI runner intermittently reports a click the page never received.
     * The component does not extend BasePage, so the loop is repeated here.
     */
    public void openCart() {
        for (int attempt = 1; attempt <= CLICK_ATTEMPTS; attempt++) {
            wait.until(ExpectedConditions.elementToBeClickable(CART_LINK)).click();

            try {
                new WebDriverWait(driver, CLICK_OUTCOME_TIMEOUT)
                        .until(ExpectedConditions.urlContains("cart.html"));
                return;
            } catch (TimeoutException e) {
                if (attempt == CLICK_ATTEMPTS) {
                    throw new IllegalStateException(
                            "Clicked the cart link " + CLICK_ATTEMPTS
                                    + " times but never reached the cart page", e);
                }
            }
        }
    }

    public void logout() {
        clickMenuItem(LOGOUT_LINK);
    }

    /**
     * Clears cart state.
     *
     * <p>Known application defect: "Reset App State" clears the badge but
     * leaves the catalogue buttons reading "Remove" until a reload. The refresh
     * is deliberate — without it a page is left whose buttons disagree with the
     * cart.
     */
    public void resetAppState() {
        clickMenuItem(RESET_LINK);
        closeMenu();
        driver.navigate().refresh();
    }

    public boolean isMenuAvailable() {
        return !driver.findElements(MENU_BUTTON).isEmpty();
    }

    /**
     * Opens the menu and clicks one of its items.
     *
     * <p>Waits on the item actually being clicked rather than on the menu
     * container: the sidebar's children reach their final position at slightly
     * different times, so a container-level check can report "open" while the
     * specific link is still travelling.
     */
    private void clickMenuItem(By item) {
        wait.until(ExpectedConditions.elementToBeClickable(MENU_BUTTON)).click();
        wait.until(ExpectedConditions.attributeToBe(MENU_WRAPPER, ARIA_HIDDEN, "false"));
        wait.until(Actionability.settledAndHittable(item));

        scriptedClick(item);
    }

    /** Closes the menu and waits for it to finish animating shut. */
    private void closeMenu() {
        wait.until(Actionability.settledAndHittable(CLOSE_MENU_BUTTON));
        scriptedClick(CLOSE_MENU_BUTTON);
        wait.until(ExpectedConditions.attributeToBe(MENU_WRAPPER, ARIA_HIDDEN, "true"));
    }

    /**
     * Clicks a slide-out menu control via script rather than natively.
     *
     * <p><b>These are the only scripted clicks in the framework</b>, and they
     * are a documented workaround rather than a shortcut. Everything else —
     * every cart, catalogue and checkout control — is driven with real clicks.
     *
     * <p>What was measured on this control, with the menu fully open and the
     * link settled at x=24, y=136, 252&times;41:
     *
     * <ul>
     *   <li>{@code document.elementFromPoint} at the link's centre returns the
     *       link itself, and its {@code pointer-events} is {@code auto}</li>
     *   <li>{@code WebElement.click()} returns without throwing, yet
     *       <em>no</em> mousedown, mouseup or click event reaches the element,
     *       and the application does not navigate</li>
     *   <li>{@code Actions.moveToElement().click()} behaves identically</li>
     *   <li>Keyboard activation with ENTER on the focused link does nothing</li>
     *   <li>The page is not scrolled — {@code window.scrollY} is 0 — so a stale
     *       scroll offset is not the cause</li>
     *   <li>A scripted click navigates correctly, every time</li>
     * </ul>
     *
     * <p>The distinguishing feature of this control is its container: the menu
     * wrapper is {@code position: fixed} and CSS-transformed, which puts it on
     * its own compositing layer. WebDriver's native click resolves a coordinate
     * that never reaches that layer, while reporting success.
     *
     * <p>A green suite that lies would be worse than this. The scripted click is
     * confined to the menu and is deliberately preceded by
     * {@link Actionability#settledAndHittable}, so the element is still proven
     * present, stationary and hit-testable first — the workaround cannot mask a
     * genuinely missing or obscured control.
     */
    private void scriptedClick(By locator) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();", driver.findElement(locator));
    }
}
