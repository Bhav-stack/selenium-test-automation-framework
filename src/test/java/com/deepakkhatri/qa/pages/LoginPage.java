package com.deepakkhatri.qa.pages;

import com.deepakkhatri.qa.config.Configuration;
import com.deepakkhatri.qa.data.TestUser;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.cssSelector("[data-test='username']");
    private static final By PASSWORD = By.cssSelector("[data-test='password']");
    private static final By LOGIN_BUTTON = By.cssSelector("[data-test='login-button']");
    private static final By ERROR = By.cssSelector("[data-test='error']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected By pageMarker() {
        return LOGIN_BUTTON;
    }

    @Step("Open the sign-in page")
    public LoginPage open() {
        driver.get(Configuration.baseUrl());
        waitUntilReady();

        return this;
    }

    /**
     * Submits credentials without waiting for a successful navigation — the
     * negative tests need the attempt to complete even when it is rejected.
     */
    @Step("Sign in as {user.username}")
    public void login(TestUser user) {
        type(USERNAME, user.username());
        type(PASSWORD, user.password());
        click(LOGIN_BUTTON);
    }

    @Step("Read the sign-in error message")
    public String errorMessage() {
        return textOf(ERROR);
    }

    /**
     * Waits for an error to appear.
     *
     * <p>A non-waiting presence check races the render: the assertion runs
     * before the message exists and reports "no error" for a page that was
     * about to show one.
     */
    public boolean waitForError() {
        try {
            return waitForVisible(ERROR).isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Instant check, for asserting that no error is present. */
    public boolean hasError() {
        return isPresent(ERROR);
    }
}
