package com.deepakkhatri.qa.pages;

import com.deepakkhatri.qa.data.Product;
import com.deepakkhatri.qa.pages.components.HeaderComponent;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class CartPage extends BasePage {

    private static final By ITEM = By.cssSelector("[data-test='inventory-item']");
    private static final By ITEM_NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By ITEM_PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By ITEM_QUANTITY = By.cssSelector("[data-test='item-quantity']");
    private static final By CHECKOUT_BUTTON = By.cssSelector("[data-test='checkout']");
    private static final By CONTINUE_SHOPPING = By.cssSelector("[data-test='continue-shopping']");

    private final HeaderComponent header;

    public CartPage(WebDriver driver) {
        super(driver);
        this.header = new HeaderComponent(driver);
    }

    @Override
    protected By pageMarker() {
        return CHECKOUT_BUTTON;
    }

    public HeaderComponent header() {
        return header;
    }

    public int itemCount() {
        return driver.findElements(ITEM).size();
    }

    public List<String> itemNames() {
        return driver.findElements(ITEM_NAME).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .toList();
    }

    private WebElement lineItem(Product product) {
        return allElements(ITEM).stream()
                .filter(item -> item.getText().contains(product.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Product not in the cart: " + product.name()));
    }

    public int quantityOf(Product product) {
        return Integer.parseInt(lineItem(product).findElement(ITEM_QUANTITY).getText().trim());
    }

    public long priceOf(Product product) {
        return parseMoneyToCents(lineItem(product).findElement(ITEM_PRICE).getText());
    }

    @Step("Remove {products} from the cart")
    public CartPage remove(Product... products) {
        for (Product product : products) {
            By removeButton = By.cssSelector("[data-test='remove-" + product.slug() + "']");
            // The line item disappearing is the proof the click landed.
            clickUntil(removeButton,
                    ExpectedConditions.invisibilityOfElementLocated(removeButton));
        }

        return this;
    }

    @Step("Proceed to checkout")
    public void proceedToCheckout() {
        clickUntil(CHECKOUT_BUTTON, ExpectedConditions.urlContains("checkout-step-one"));
    }

    @Step("Continue shopping")
    public void continueShopping() {
        clickUntil(CONTINUE_SHOPPING, ExpectedConditions.urlContains("inventory"));
    }
}
