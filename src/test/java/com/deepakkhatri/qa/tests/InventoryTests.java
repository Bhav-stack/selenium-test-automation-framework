package com.deepakkhatri.qa.tests;

import com.deepakkhatri.qa.base.BaseTest;
import com.deepakkhatri.qa.data.DataProviders;
import com.deepakkhatri.qa.data.Products;
import com.deepakkhatri.qa.data.SortOption;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.testng.Assert.assertEquals;

@Epic("Catalogue")
@Feature("Product listing and sorting")
public class InventoryTests extends BaseTest {

    @Test(groups = {"smoke", "regression"},
            description = "Every catalogue item is listed")
    @Severity(SeverityLevel.CRITICAL)
    public void allProductsAreListed() {
        signIn();

        assertEquals(inventoryPage().itemCount(), Products.ALL.size(),
                "Number of products in the catalogue");
    }

    @Test(groups = {"regression"}, description = "Sorting by name orders the grid alphabetically")
    public void sortByNameAscending() {
        signIn().sortBy(SortOption.NAME_ASCENDING);

        List<String> actual = inventoryPage().productNames();
        List<String> expected = new ArrayList<>(actual);
        expected.sort(Comparator.naturalOrder());

        assertEquals(actual, expected, "Product names sorted A to Z");
    }

    @Test(groups = {"regression"}, description = "Sorting by name Z to A reverses the order")
    public void sortByNameDescending() {
        signIn().sortBy(SortOption.NAME_DESCENDING);

        List<String> actual = inventoryPage().productNames();
        List<String> expected = new ArrayList<>(actual);
        expected.sort(Comparator.reverseOrder());

        assertEquals(actual, expected, "Product names sorted Z to A");
    }

    @Test(groups = {"smoke", "regression"},
            description = "Sorting by price low to high orders by ascending price")
    public void sortByPriceAscending() {
        signIn().sortBy(SortOption.PRICE_LOW_TO_HIGH);

        List<Long> actual = inventoryPage().productPricesInCents();
        List<Long> expected = new ArrayList<>(actual);
        expected.sort(Comparator.naturalOrder());

        assertEquals(actual, expected, "Prices sorted low to high");
    }

    @Test(groups = {"regression"},
            description = "Sorting by price high to low orders by descending price")
    public void sortByPriceDescending() {
        signIn().sortBy(SortOption.PRICE_HIGH_TO_LOW);

        List<Long> actual = inventoryPage().productPricesInCents();
        List<Long> expected = new ArrayList<>(actual);
        expected.sort(Comparator.reverseOrder());

        assertEquals(actual, expected, "Prices sorted high to low");
    }

    /**
     * Whatever the order, the catalogue must still contain the same products.
     * A sort that dropped or duplicated an item would pass an ordering check
     * on its own.
     */
    @Test(dataProvider = "sortOptions", dataProviderClass = DataProviders.class,
            groups = {"regression"},
            description = "Sorting preserves the set of products")
    public void sortingPreservesTheProductSet(SortOption option) {
        signIn();
        List<String> before = new ArrayList<>(inventoryPage().productNames());

        inventoryPage().sortBy(option);
        List<String> after = new ArrayList<>(inventoryPage().productNames());

        before.sort(Comparator.naturalOrder());
        after.sort(Comparator.naturalOrder());

        assertEquals(after, before, "Product set after sorting by " + option);
    }
}
