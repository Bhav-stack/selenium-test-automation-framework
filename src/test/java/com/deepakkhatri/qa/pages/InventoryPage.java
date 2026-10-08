package com.deepakkhatri.qa.pages;

import com.deepakkhatri.qa.data.Product;
import com.deepakkhatri.qa.data.SortOption;
import com.deepakkhatri.qa.pages.components.HeaderComponent;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class InventoryPage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By ITEM = By.cssSelector("[data-test='inventory-item']");
    private static final By ITEM_NAME = By.cssSelector("[data-test='inventory-item-name']");
    private static final By ITEM_PRICE = By.cssSelector("[data-test='inventory-item-price']");
    private static final By SORT_DROPDOWN = By.cssSelector("[data-test='product-sort-container']");

    private final HeaderComponent header;

    public InventoryPage(WebDriver driver) {
        super(driver);
        this.header = new HeaderComponent(driver);
    }

    @Override
    protected By pageMarker() {
        return SORT_DROPDOWN;
    }

    public HeaderComponent header() {
        return header;
    }

    public String title() {
        return textOf(TITLE);
    }

    public int itemCount() {
        return allElements(ITEM).size();
    }

    private By addToCartButton(Product product) {
        return By.cssSelector("[data-test='add-to-cart-" + product.slug() + "']");
    }

    private By removeButton(Product product) {
        return By.cssSelector("[data-test='remove-" + product.slug() + "']");
    }

    @Step("Add {products} to the cart")
    public InventoryPage addToCart(Product... products) {
        for (Product product : products) {
            // The button swapping to "Remove" is the proof the click landed.
            clickUntil(addToCartButton(product),
                    ExpectedConditions.presenceOfElementLocated(removeButton(product)));
        }

        return this;
    }

    @Step("Remove {products} from the cart")
    public InventoryPage removeFromCart(Product... products) {
        for (Product product : products) {
            clickUntil(removeButton(product),
                    ExpectedConditions.presenceOfElementLocated(addToCartButton(product)));
        }

        return this;
    }

    public boolean isRemoveButtonShown(Product product) {
        return isPresent(removeButton(product));
    }

    public boolean isAddButtonShown(Product product) {
        return isPresent(addToCartButton(product));
    }

    @Step("Sort the catalogue by {option}")
    public InventoryPage sortBy(SortOption option) {
        selectByValue(SORT_DROPDOWN, option.value());

        return this;
    }

    /** Product names in the order the grid currently renders them. */
    public List<String> productNames() {
        return allElements(ITEM_NAME).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .toList();
    }

    /** Prices in cents, in the order the grid currently renders them. */
    public List<Long> productPricesInCents() {
        return allElements(ITEM_PRICE).stream()
                .map(WebElement::getText)
                .map(BasePage::parseMoneyToCents)
                .toList();
    }

    public long priceOf(Product product) {
        WebElement card = allElements(ITEM).stream()
                .filter(item -> item.getText().contains(product.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Product not found in the catalogue: " + product.name()));

        return parseMoneyToCents(card.findElement(ITEM_PRICE).getText());
    }
}
