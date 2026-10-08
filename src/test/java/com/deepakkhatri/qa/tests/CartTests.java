package com.deepakkhatri.qa.tests;

import com.deepakkhatri.qa.base.BaseTest;
import com.deepakkhatri.qa.data.Products;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

@Epic("Shopping cart")
@Feature("Cart state")
public class CartTests extends BaseTest {

    @Test(groups = {"smoke", "regression"},
            description = "Adding a product updates the badge and swaps the button to Remove")
    @Severity(SeverityLevel.CRITICAL)
    public void addingProductUpdatesBadgeAndButton() {
        signIn().addToCart(Products.BACKPACK);

        assertEquals(inventoryPage().header().cartCount(), 1, "Cart badge after adding one product");
        assertTrue(inventoryPage().isRemoveButtonShown(Products.BACKPACK),
                "Remove button shown after adding");
        assertFalse(inventoryPage().isAddButtonShown(Products.BACKPACK),
                "Add button hidden after adding");
    }

    @Test(groups = {"regression"}, description = "The badge counts every distinct product added")
    public void badgeCountsEveryProduct() {
        signIn().addToCart(Products.BACKPACK, Products.BIKE_LIGHT, Products.FLEECE_JACKET);

        assertEquals(inventoryPage().header().cartCount(), 3, "Cart badge after adding three products");
    }

    @Test(groups = {"regression"}, description = "Removing the last product clears the badge")
    public void removingLastProductClearsBadge() {
        signIn().addToCart(Products.ONESIE);
        assertTrue(inventoryPage().header().isCartBadgeVisible(), "Badge visible after adding");

        inventoryPage().removeFromCart(Products.ONESIE);

        assertFalse(inventoryPage().header().isCartBadgeVisible(), "Badge hidden once the cart is empty");
        assertEquals(inventoryPage().header().cartCount(), 0, "Cart count once empty");
    }

    @Test(groups = {"smoke", "regression"},
            description = "The cart page lists exactly the products that were added")
    public void cartListsAddedProducts() {
        signIn().addToCart(Products.BACKPACK, Products.BOLT_T_SHIRT);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();

        assertEquals(cartPage().itemCount(), 2, "Line items in the cart");
        assertTrue(cartPage().itemNames().contains(Products.BACKPACK.name()),
                "Backpack present in the cart");
        assertTrue(cartPage().itemNames().contains(Products.BOLT_T_SHIRT.name()),
                "Bolt T-Shirt present in the cart");
        assertEquals(cartPage().quantityOf(Products.BACKPACK), 1, "Quantity for a single add");
    }

    @Test(groups = {"regression"},
            description = "Cart contents survive navigating back to the catalogue")
    public void cartSurvivesNavigation() {
        signIn().addToCart(Products.FLEECE_JACKET);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();

        cartPage().continueShopping();
        inventoryPage().waitUntilReady();

        assertEquals(inventoryPage().header().cartCount(), 1, "Cart badge after returning to the catalogue");
        assertTrue(inventoryPage().isRemoveButtonShown(Products.FLEECE_JACKET),
                "Button still reads Remove after returning");
    }

    @Test(groups = {"regression"}, description = "Removing from the cart page updates the badge")
    public void removingFromCartPageUpdatesBadge() {
        signIn().addToCart(Products.BACKPACK, Products.BIKE_LIGHT);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();

        cartPage().remove(Products.BIKE_LIGHT);

        assertEquals(cartPage().itemCount(), 1, "Line items after removing one");
        assertEquals(cartPage().header().cartCount(), 1, "Cart badge after removing one");
    }

    @Test(groups = {"regression"},
            description = "The price in the cart matches the catalogue price")
    public void cartPriceMatchesCataloguePrice() {
        signIn();
        long cataloguePrice = inventoryPage().priceOf(Products.FLEECE_JACKET);

        inventoryPage().addToCart(Products.FLEECE_JACKET);
        inventoryPage().header().openCart();
        cartPage().waitUntilReady();

        assertEquals(cartPage().priceOf(Products.FLEECE_JACKET), cataloguePrice,
                "Cart price must match the catalogue price, in cents");
    }
}
