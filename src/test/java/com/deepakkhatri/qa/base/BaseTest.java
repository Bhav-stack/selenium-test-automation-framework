package com.deepakkhatri.qa.base;

import com.deepakkhatri.qa.config.Configuration;
import com.deepakkhatri.qa.data.TestUser;
import com.deepakkhatri.qa.data.Users;
import com.deepakkhatri.qa.driver.DriverFactory;
import com.deepakkhatri.qa.driver.DriverManager;
import com.deepakkhatri.qa.listeners.TestListener;
import com.deepakkhatri.qa.pages.CartPage;
import com.deepakkhatri.qa.pages.CheckoutPages;
import com.deepakkhatri.qa.pages.InventoryPage;
import com.deepakkhatri.qa.pages.LoginPage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

/**
 * Driver lifecycle and page-object access for every test class.
 *
 * <p><b>Page objects are exposed as methods, not fields, and this is the most
 * important design decision in the framework.</b> TestNG instantiates a test
 * class <em>once</em> and, under {@code parallel="methods"}, runs its methods
 * concurrently against that single instance. Instance fields are therefore
 * shared across threads: a {@code protected LoginPage loginPage} assigned in
 * {@code @BeforeMethod} is overwritten by whichever thread starts next, and
 * tests end up driving each other's browsers.
 *
 * <p>That bug was real in this repository — it produced three failures whose
 * symptoms (a locked-out user receiving a bad-credentials message, timeouts
 * waiting for pages that had already loaded) pointed at the application rather
 * than at the framework. A {@link ThreadLocal} driver alone does not prevent
 * it, because the shared field holds a page object that has already captured
 * the wrong driver.
 *
 * <p>Building page objects on demand from the calling thread's driver removes
 * the shared state entirely. They are thin wrappers over a driver reference,
 * so construction is effectively free.
 *
 * <p>{@code @BeforeMethod} rather than {@code @BeforeClass} for the driver: a
 * fresh browser per test costs launch time and buys isolation, which is the
 * right trade for a suite this size.
 */
@Listeners(TestListener.class)
public abstract class BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void startDriver() {
        DriverManager.set(DriverFactory.create());
    }

    /**
     * {@code alwaysRun = true} so the browser closes even when setup failed.
     * Without it, a failing {@code @BeforeMethod} leaks a browser process per
     * test and a full run can exhaust the machine.
     */
    @AfterMethod(alwaysRun = true)
    public void stopDriver() {
        DriverManager.quit();
    }

    protected LoginPage loginPage() {
        return new LoginPage(DriverManager.get());
    }

    protected InventoryPage inventoryPage() {
        return new InventoryPage(DriverManager.get());
    }

    protected CartPage cartPage() {
        return new CartPage(DriverManager.get());
    }

    protected CheckoutPages.Information checkoutInformation() {
        return new CheckoutPages.Information(DriverManager.get());
    }

    protected CheckoutPages.Overview checkoutOverview() {
        return new CheckoutPages.Overview(DriverManager.get());
    }

    protected CheckoutPages.Complete checkoutComplete() {
        return new CheckoutPages.Complete(DriverManager.get());
    }

    /** Signs in and returns the catalogue, for tests whose subject is not login. */
    protected InventoryPage signInAs(TestUser user) {
        LoginPage login = loginPage();
        login.open();
        login.login(user);

        InventoryPage inventory = inventoryPage();
        inventory.waitUntilReady();

        return inventory;
    }

    protected InventoryPage signIn() {
        return signInAs(Users.standard());
    }

    protected String baseUrl() {
        return Configuration.baseUrl();
    }

    /** Navigates straight to a path, bypassing the UI — used by the access-control tests. */
    protected void navigateTo(String path) {
        DriverManager.get().get(Configuration.baseUrl() + path);
    }
}
