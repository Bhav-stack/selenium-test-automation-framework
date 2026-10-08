package com.deepakkhatri.qa.config;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Browsers the framework knows how to launch. */
public enum Browser {
    CHROME,
    FIREFOX,
    EDGE;

    /**
     * Parses a configured browser name, failing loudly on an unknown value.
     *
     * <p>Silently defaulting to Chrome on a typo would report a green
     * cross-browser run that never touched the other browser.
     */
    public static Browser from(String value) {
        return Arrays.stream(values())
                .filter(browser -> browser.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown browser '" + value + "'. Supported: "
                                + Arrays.stream(values())
                                .map(browser -> browser.name().toLowerCase())
                                .collect(Collectors.joining(", "))));
    }
}
