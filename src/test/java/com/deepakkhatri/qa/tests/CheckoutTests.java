package com.deepakkhatri.qa.tests;

import com.deepakkhatri.qa.base.BaseTest;
import com.deepakkhatri.qa.data.CheckoutDetails;
import com.deepakkhatri.qa.data.DataProviders;
import com.deepakkhatri.qa.data.Products;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Epic("Checkout")
@Feature("Order placement")
public class CheckoutTests extends BaseTest {

    @Test(groups = {"smoke", "regression", "e2e"},
            description = "A customer can complete an order end to end")
    @Severity(SeverityLevel.BLOCKER)
    @Story("Happy path")
    public void customerCanCompleteAnOrder() {
        signIn().addToCart(Products.BACKPACK, Products.BIKE_LIGHT);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();

        assertEquals(cartPage().itemCount(), 2, "Line items before checkout");

        cartPage().proceedToCheckout();
        checkoutInformation().submit(CheckoutDetails.valid());

        checkoutOverview().waitUntilReady();
        assertEquals(checkoutOverview().itemCount(), 2, "Line items on the order summary");

        checkoutOverview().finish();

        checkoutComplete().waitUntilReady();
        assertEquals(checkoutComplete().confirmationMessage(), "Thank you for your order!",
                "Order confirmation message");
    }

    /**
     * Both halves of the arithmetic are checked. Asserting only the total would
     * let a compensating error in the subtotal or the tax pass unnoticed.
     */
    @Test(groups = {"regression", "e2e"},
            description = "The order total equals the line items plus the quoted tax")
    @Severity(SeverityLevel.CRITICAL)
    public void orderTotalIsArithmeticallyCorrect() {
        signIn().addToCart(Products.BACKPACK, Products.FLEECE_JACKET, Products.ONESIE);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();
        cartPage().proceedToCheckout();
        checkoutInformation().submit(CheckoutDetails.valid());
        checkoutOverview().waitUntilReady();

        long subtotal = checkoutOverview().subtotalCents();
        long tax = checkoutOverview().taxCents();
        long total = checkoutOverview().totalCents();
        long lineItems = checkoutOverview().lineItemTotalCents();

        assertEquals(subtotal, lineItems,
                "Subtotal must account for every line item (cents)");
        assertEquals(total, subtotal + tax,
                "Total must equal subtotal plus tax (cents)");
    }

    @Test(dataProvider = "missingCheckoutFields", dataProviderClass = DataProviders.class,
            groups = {"regression"},
            description = "Checkout is blocked when a required field is missing")
    @Severity(SeverityLevel.CRITICAL)
    public void checkoutBlockedWhenFieldMissing(
            String fieldName, CheckoutDetails details, String expectedError) {

        signIn().addToCart(Products.BACKPACK);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();
        cartPage().proceedToCheckout();

        checkoutInformation().submit(details);

        assertEquals(checkoutInformation().errorMessage(), expectedError,
                "Validation message when the " + fieldName + " is missing");
        assertTrue(checkoutInformation().currentUrl().contains("/checkout-step-one.html"),
                "Checkout must not advance past step one when the " + fieldName + " is missing");
    }

    @Test(groups = {"regression"},
            description = "Leaving the summary keeps the cart intact")
    public void leavingSummaryKeepsCartIntact() {
        signIn().addToCart(Products.BOLT_T_SHIRT);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();
        cartPage().proceedToCheckout();
        checkoutInformation().submit(CheckoutDetails.valid());
        checkoutOverview().waitUntilReady();

        checkoutOverview().header().openCart();
        cartPage().waitUntilReady();

        assertEquals(cartPage().header().cartCount(), 1, "Cart badge after leaving the summary");
        assertEquals(cartPage().itemCount(), 1, "Line items after leaving the summary");
    }

    @Test(groups = {"regression", "e2e"}, description = "A confirmed order empties the cart")
    public void confirmedOrderEmptiesCart() {
        signIn().addToCart(Products.ONESIE);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();
        cartPage().proceedToCheckout();
        checkoutInformation().submit(CheckoutDetails.valid());
        checkoutOverview().waitUntilReady();
        checkoutOverview().finish();

        checkoutComplete().waitUntilReady();

        assertEquals(checkoutComplete().header().cartCount(), 0,
                "Cart must be empty once the order is confirmed");
    }
}
