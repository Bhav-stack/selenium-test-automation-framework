package com.deepakkhatri.qa.data;

/**
 * A catalogue item. {@code slug} is the suffix the application derives from the
 * product name for its {@code data-test} hooks.
 */
public record Product(String name, String slug) {

    public static Product of(String name) {
        return new Product(name, name.toLowerCase().replaceAll("\\s+", "-"));
    }

    @Override
    public String toString() {
        return name;
    }
}
