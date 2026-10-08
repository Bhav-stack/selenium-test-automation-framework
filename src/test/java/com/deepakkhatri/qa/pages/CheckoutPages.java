package com.deepakkhatri.qa.pages;

import com.deepakkhatri.qa.data.CheckoutDetails;
import com.deepakkhatri.qa.pages.components.HeaderComponent;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * The three checkout steps. Grouped in one file because they are only ever
 * used as a sequence, and splitting them across three files would separate
 * the flow without making any of them easier to find.
 */
public final class CheckoutPages {

    private CheckoutPages() {
        // Container for the three step classes.
    }

    /** Step one — customer information. */
    public static class Information extends BasePage {

        private static final By FIRST_NAME = By.cssSelector("[data-test='firstName']");
        private static final By LAST_NAME = By.cssSelector("[data-test='lastName']");
        private static final By POSTAL_CODE = By.cssSelector("[data-test='postalCode']");
        private static final By CONTINUE = By.cssSelector("[data-test='continue']");
        private static final By CANCEL = By.cssSelector("[data-test='cancel']");
        private static final By ERROR = By.cssSelector("[data-test='error']");

        public Information(WebDriver driver) {
            super(driver);
        }

        @Override
        protected By pageMarker() {
            return CONTINUE;
        }

        @Step("Submit checkout details for {details.firstName} {details.lastName}")
        public void submit(CheckoutDetails details) {
            type(FIRST_NAME, details.firstName());
            type(LAST_NAME, details.lastName());
            type(POSTAL_CODE, details.postalCode());
            click(CONTINUE);
        }

        public String errorMessage() {
            return textOf(ERROR);
        }

        public void cancel() {
            click(CANCEL);
        }
    }

    /** Step two — the order summary, where the money is checked. */
    public static class Overview extends BasePage {

        private static final By ITEM = By.cssSelector("[data-test='inventory-item']");
        private static final By ITEM_PRICE = By.cssSelector("[data-test='inventory-item-price']");
        private static final By SUBTOTAL = By.cssSelector("[data-test='subtotal-label']");
        private static final By TAX = By.cssSelector("[data-test='tax-label']");
        private static final By TOTAL = By.cssSelector("[data-test='total-label']");
        private static final By FINISH = By.cssSelector("[data-test='finish']");

        private final HeaderComponent header;

        public Overview(WebDriver driver) {
            super(driver);
            this.header = new HeaderComponent(driver);
        }

        @Override
        protected By pageMarker() {
            return FINISH;
        }

        public HeaderComponent header() {
            return header;
        }

        public int itemCount() {
            return driver.findElements(ITEM).size();
        }

        public long subtotalCents() {
            return parseMoneyToCents(textOf(SUBTOTAL));
        }

        public long taxCents() {
            return parseMoneyToCents(textOf(TAX));
        }

        public long totalCents() {
            return parseMoneyToCents(textOf(TOTAL));
        }

        /** Sum of the line items as rendered on this page, in cents. */
        public long lineItemTotalCents() {
            return allElements(ITEM_PRICE).stream()
                    .map(WebElement::getText)
                    .mapToLong(BasePage::parseMoneyToCents)
                    .sum();
        }

        @Step("Finish the order")
        public void finish() {
            clickUntil(FINISH, ExpectedConditions.urlContains("checkout-complete"));
        }
    }

    /** Step three — order confirmation. */
    public static class Complete extends BasePage {

        private static final By CONFIRMATION = By.cssSelector("[data-test='complete-header']");
        private static final By BACK_TO_PRODUCTS = By.cssSelector("[data-test='back-to-products']");

        private final HeaderComponent header;

        public Complete(WebDriver driver) {
            super(driver);
            this.header = new HeaderComponent(driver);
        }

        @Override
        protected By pageMarker() {
            return CONFIRMATION;
        }

        public HeaderComponent header() {
            return header;
        }

        public String confirmationMessage() {
            return textOf(CONFIRMATION);
        }

        public void backToProducts() {
            click(BACK_TO_PRODUCTS);
        }
    }
}
