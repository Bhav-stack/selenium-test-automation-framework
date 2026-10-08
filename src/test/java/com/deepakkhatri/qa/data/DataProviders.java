package com.deepakkhatri.qa.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * TestNG data providers, backed by JSON on the classpath.
 *
 * <p>Externalising the cases means a new scenario is a JSON entry rather than a
 * code change, and each row becomes its own test in the report — a regression
 * names the exact input that broke instead of collapsing six scenarios into one
 * red line.
 */
public final class DataProviders {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DataProviders() {
        // Utility class.
    }

    /** One rejected sign-in scenario. */
    public record InvalidLogin(
            String username,
            String password,
            String description,
            String expectedError) {

        public TestUser asUser() {
            return new TestUser(username, password, description);
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @DataProvider(name = "invalidLogins")
    public static Object[][] invalidLogins() throws IOException {
        List<InvalidLogin> cases = readList("testdata/invalid-logins.json", InvalidLogin[].class);

        return cases.stream()
                .map(entry -> new Object[]{entry})
                .toArray(Object[][]::new);
    }

    /** Checkout cases with one required field blanked out. */
    @DataProvider(name = "missingCheckoutFields")
    public static Object[][] missingCheckoutFields() {
        return new Object[][]{
                {"first name", CheckoutDetails.builder().firstName("").build(),
                        CheckoutDetails.Errors.FIRST_NAME_REQUIRED},
                {"last name", CheckoutDetails.builder().lastName("").build(),
                        CheckoutDetails.Errors.LAST_NAME_REQUIRED},
                {"postal code", CheckoutDetails.builder().postalCode("").build(),
                        CheckoutDetails.Errors.POSTAL_CODE_REQUIRED},
        };
    }

    @DataProvider(name = "sortOptions")
    public static Object[][] sortOptions() {
        return new Object[][]{
                {SortOption.NAME_ASCENDING},
                {SortOption.NAME_DESCENDING},
                {SortOption.PRICE_LOW_TO_HIGH},
                {SortOption.PRICE_HIGH_TO_LOW},
        };
    }

    private static <T> List<T> readList(String resource, Class<T[]> type) throws IOException {
        try (InputStream stream =
                     DataProviders.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Test data not found on the classpath: " + resource);
            }

            return List.of(MAPPER.readValue(stream, type));
        }
    }
}
