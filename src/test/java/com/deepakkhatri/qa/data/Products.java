package com.deepakkhatri.qa.data;

import java.util.List;

/**
 * The catalogue, used to <em>select</em> products. Money assertions read
 * prices back off the page instead of comparing against constants here — a
 * hardcoded price turns a catalogue update into a false failure.
 */
public final class Products {

    public static final Product BACKPACK = Product.of("Sauce Labs Backpack");
    public static final Product BIKE_LIGHT = Product.of("Sauce Labs Bike Light");
    public static final Product BOLT_T_SHIRT = Product.of("Sauce Labs Bolt T-Shirt");
    public static final Product FLEECE_JACKET = Product.of("Sauce Labs Fleece Jacket");
    public static final Product ONESIE = Product.of("Sauce Labs Onesie");
    public static final Product RED_T_SHIRT = Product.of("Test.allTheThings() T-Shirt (Red)");

    public static final List<Product> ALL = List.of(
            BACKPACK, BIKE_LIGHT, BOLT_T_SHIRT, FLEECE_JACKET, ONESIE, RED_T_SHIRT);

    private Products() {
        // Constant holder.
    }
}
