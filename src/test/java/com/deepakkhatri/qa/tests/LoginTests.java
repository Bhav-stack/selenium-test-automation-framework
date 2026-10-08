package com.deepakkhatri.qa.tests;

import com.deepakkhatri.qa.base.BaseTest;
import com.deepakkhatri.qa.data.DataProviders;
import com.deepakkhatri.qa.data.Users;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Epic("Authentication")
@Feature("Sign-in")
public class LoginTests extends BaseTest {

    @Test(groups = {"smoke", "regression"},
            description = "A valid user reaches the product catalogue")
    @Severity(SeverityLevel.BLOCKER)
    @Description("The primary sign-in path — if this fails, nothing else can run.")
    public void validUserReachesCatalogue() {
        loginPage().open();
        loginPage().login(Users.standard());

        inventoryPage().waitUntilReady();
        assertEquals(inventoryPage().title(), "Products",
                "Catalogue heading after a successful sign-in");
        assertTrue(inventoryPage().currentUrl().contains("/inventory.html"),
                "URL after a successful sign-in: " + inventoryPage().currentUrl());
    }

    @Test(groups = {"smoke", "regression"},
            description = "A locked-out account is refused with an explanatory message")
    @Severity(SeverityLevel.CRITICAL)
    public void lockedOutUserIsRefused() {
        loginPage().open();
        loginPage().login(Users.lockedOut());

        assertEquals(loginPage().errorMessage(), Users.Errors.LOCKED_OUT,
                "Error shown for a locked-out account");
        assertFalse(loginPage().currentUrl().contains("/inventory.html"),
                "A locked-out user must not reach the catalogue");
    }

    /**
     * Cases come from {@code testdata/invalid-logins.json}, so each row is a
     * separate test in the report and a new scenario needs no code change.
     */
    @Test(dataProvider = "invalidLogins", dataProviderClass = DataProviders.class,
            groups = {"regression"},
            description = "Sign-in is rejected for invalid credentials")
    @Severity(SeverityLevel.CRITICAL)
    public void invalidCredentialsAreRejected(DataProviders.InvalidLogin scenario) {
        loginPage().open();
        loginPage().login(scenario.asUser());

        assertEquals(loginPage().errorMessage(), scenario.expectedError(),
                "Error shown for: " + scenario.description());
        assertFalse(loginPage().currentUrl().contains("/inventory.html"),
                "Rejected credentials must not reach the catalogue: " + scenario.description());
    }

    @Test(groups = {"regression"},
            description = "Signing out returns the user to the sign-in form")
    public void signOutReturnsToSignInForm() {
        signIn();

        inventoryPage().header().logout();

        assertTrue(loginPage().isDisplayed(), "Sign-in form after signing out");
    }

    @Test(groups = {"regression", "security"},
            description = "A signed-out user cannot reach the catalogue by URL")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Direct navigation to a protected page must be refused rather than rendered.")
    public void directUrlAccessIsRefused() {
        // Request the protected page without signing in first.
        navigateTo("/inventory.html");

        assertTrue(loginPage().waitForError(),
                "An error must be shown when a protected page is requested directly");

        String message = loginPage().errorMessage();
        assertTrue(message.contains("You can only access"),
                "Access-control message, got: " + message);
    }
}
