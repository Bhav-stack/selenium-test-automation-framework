package com.deepakkhatri.qa.data;

/**
 * Checkout form values, built through {@link #builder()} so a test states only
 * the field under test and inherits valid values for the rest.
 */
public record CheckoutDetails(String firstName, String lastName, String postalCode) {

    public static Builder builder() {
        return new Builder();
    }

    public static CheckoutDetails valid() {
        return builder().build();
    }

    public static final class Builder {

        private String firstName = "Deepak";
        private String lastName = "Khatri";
        private String postalCode = "3220";

        public Builder firstName(String value) {
            this.firstName = value;
            return this;
        }

        public Builder lastName(String value) {
            this.lastName = value;
            return this;
        }

        public Builder postalCode(String value) {
            this.postalCode = value;
            return this;
        }

        public CheckoutDetails build() {
            return new CheckoutDetails(firstName, lastName, postalCode);
        }
    }

    /** Validation copy returned when a checkout field is left blank. */
    public static final class Errors {

        public static final String FIRST_NAME_REQUIRED = "Error: First Name is required";
        public static final String LAST_NAME_REQUIRED = "Error: Last Name is required";
        public static final String POSTAL_CODE_REQUIRED = "Error: Postal Code is required";

        private Errors() {
            // Constant holder.
        }
    }
}
