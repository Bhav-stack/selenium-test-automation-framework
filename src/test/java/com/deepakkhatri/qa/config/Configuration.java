package com.deepakkhatri.qa.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Central configuration, resolved once at class-load time.
 *
 * <p>Resolution order is system property, then environment variable, then
 * {@code config.properties}. That ordering lets CI override any value with
 * {@code -Dbrowser=firefox} without editing a file, while a developer gets
 * working defaults from a clean checkout.
 */
public final class Configuration {

    private static final Properties FILE_PROPERTIES = load();

    private Configuration() {
        // Utility class.
    }

    private static Properties load() {
        Properties properties = new Properties();

        try (InputStream stream =
                     Configuration.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read config.properties", e);
        }

        return properties;
    }

    /**
     * Reads a value, treating blank as unset.
     *
     * <p>An unset CI secret or an empty {@code -D} flag arrives as an empty
     * string rather than as null. Falling back only on null would silently
     * adopt {@code ""} as the configured value — the failure mode this method
     * exists to prevent.
     */
    private static String resolve(String key, String fallback) {
        String fromSystem = System.getProperty(key);
        if (isPresent(fromSystem)) {
            return fromSystem;
        }

        String fromEnv = System.getenv(key.toUpperCase().replace('.', '_'));
        if (isPresent(fromEnv)) {
            return fromEnv;
        }

        String fromFile = FILE_PROPERTIES.getProperty(key);
        return isPresent(fromFile) ? fromFile : fallback;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    public static String baseUrl() {
        return resolve("base.url", "https://www.saucedemo.com");
    }

    public static Browser browser() {
        return Browser.from(resolve("browser", "chrome"));
    }

    /** {@code local} runs browsers on this machine; {@code grid} uses a Selenium Grid. */
    public static boolean isGridExecution() {
        return "grid".equalsIgnoreCase(resolve("execution.mode", "local"));
    }

    public static String gridUrl() {
        return resolve("grid.url", "http://localhost:4444/wd/hub");
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(resolve("headless", "true"));
    }

    /**
     * Ceiling for explicit waits. Every wait in the framework is explicit —
     * there is no implicit wait, because mixing the two produces wait times
     * that are neither value and are notoriously hard to diagnose.
     */
    public static Duration explicitWait() {
        return Duration.ofSeconds(Long.parseLong(resolve("wait.seconds", "15")));
    }

    public static Duration pageLoadTimeout() {
        return Duration.ofSeconds(Long.parseLong(resolve("pageload.seconds", "30")));
    }

    public static String password() {
        return resolve("user.password", "secret_sauce");
    }
}
