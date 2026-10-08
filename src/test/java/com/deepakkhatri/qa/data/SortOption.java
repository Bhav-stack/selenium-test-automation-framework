package com.deepakkhatri.qa.data;

/** Sort options offered by the catalogue dropdown. */
public enum SortOption {

    NAME_ASCENDING("az"),
    NAME_DESCENDING("za"),
    PRICE_LOW_TO_HIGH("lohi"),
    PRICE_HIGH_TO_LOW("hilo");

    private final String value;

    SortOption(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
