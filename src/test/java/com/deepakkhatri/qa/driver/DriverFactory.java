package com.deepakkhatri.qa.driver;

import com.deepakkhatri.qa.config.Browser;
import com.deepakkhatri.qa.config.Configuration;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;

/**
 * Builds a WebDriver for the configured browser and execution mode.
 *
 * <p>The same options objects serve both modes: local execution passes them to
 * a concrete driver, grid execution passes them to a {@link RemoteWebDriver}.
 * Keeping one source of browser configuration is what makes a local run and a
 * grid run comparable — divergent options are a classic reason a suite passes
 * on a developer machine and fails in CI.
 */
public final class DriverFactory {

    private DriverFactory() {
        // Utility class.
    }

    public static WebDriver create() {
        Browser browser = Configuration.browser();
        MutableCapabilities options = optionsFor(browser);

        WebDriver driver = Configuration.isGridExecution()
                ? remoteDriver(options)
                : localDriver(browser, options);

        driver.manage().timeouts().pageLoadTimeout(Configuration.pageLoadTimeout());

        /*
         * Explicit size rather than maximize(). In headless Chrome, maximize()
         * silently overrides the --window-size argument and leaves an 800x457
         * viewport — measured, not assumed. Layout-sensitive elements then sit
         * at different coordinates locally and in CI, and screenshots come out
         * at an unexpected size.
         */
        driver.manage().window().setSize(new Dimension(1920, 1080));

        return driver;
    }

    private static MutableCapabilities optionsFor(Browser browser) {
        boolean headless = Configuration.isHeadless();

        return switch (browser) {
            case CHROME -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080");
                /* Required in containers: the default /dev/shm is 64 MB, and
                   Chrome crashes mid-run when it fills. The single most common
                   cause of "works locally, dies in CI" with Selenium. */
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-gpu");
                yield options;
            }
            case FIREFOX -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                options.addArguments("--width=1920", "--height=1080");
                yield options;
            }
            case EDGE -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--disable-dev-shm-usage");
                yield options;
            }
        };
    }

    /* Selenium Manager resolves and caches the matching driver binary, so
       there is no WebDriverManager dependency and nothing checked in. */
    private static WebDriver localDriver(Browser browser, MutableCapabilities options) {
        return switch (browser) {
            case CHROME -> new ChromeDriver((ChromeOptions) options);
            case FIREFOX -> new FirefoxDriver((FirefoxOptions) options);
            case EDGE -> new EdgeDriver((EdgeOptions) options);
        };
    }

    private static WebDriver remoteDriver(MutableCapabilities options) {
        String gridUrl = Configuration.gridUrl();

        try {
            return new RemoteWebDriver(URI.create(gridUrl).toURL(), options);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid grid URL: " + gridUrl, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Could not reach the Selenium Grid at " + gridUrl
                            + ". Start it with: docker compose -f docker-compose.grid.yml up -d", e);
        }
    }
}
