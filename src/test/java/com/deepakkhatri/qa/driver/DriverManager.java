package com.deepakkhatri.qa.driver;

import org.openqa.selenium.WebDriver;

/**
 * Thread-confined WebDriver storage.
 *
 * <p>This is the piece that makes parallel execution correct. TestNG runs each
 * test method on its own thread; a {@code static WebDriver} field would be
 * shared across all of them, so threads would steal each other's browsers and
 * produce failures that look like application defects but are not.
 * A {@link ThreadLocal} gives each thread its own driver.
 *
 * <p>{@link #remove()} is not optional. Surefire reuses threads between test
 * methods, so a ThreadLocal that is never cleared leaks the previous driver
 * reference into the next test — which then quits an already-quit browser.
 */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // Utility class.
    }

    public static WebDriver get() {
        WebDriver driver = DRIVER.get();

        if (driver == null) {
            throw new IllegalStateException(
                    "No WebDriver bound to thread '" + Thread.currentThread().getName()
                            + "'. A test is running outside BaseTest's lifecycle.");
        }

        return driver;
    }

    public static boolean isSet() {
        return DRIVER.get() != null;
    }

    public static void set(WebDriver driver) {
        DRIVER.set(driver);
    }

    /** Quits the driver and clears the slot, even if quitting throws. */
    public static void quit() {
        WebDriver driver = DRIVER.get();

        if (driver == null) {
            return;
        }

        try {
            driver.quit();
        } finally {
            DRIVER.remove();
        }
    }
}
