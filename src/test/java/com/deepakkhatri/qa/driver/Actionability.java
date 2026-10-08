package com.deepakkhatri.qa.driver;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.util.List;
import java.util.Map;

/**
 * The actionability checks Selenium does not provide.
 *
 * <p>{@code ExpectedConditions.elementToBeClickable} answers only "is this
 * element displayed and enabled". Two things it does not answer, and both
 * caused real failures in this repository:
 *
 * <ol>
 *   <li><b>Has it stopped moving?</b> Selenium computes a click coordinate and
 *       then dispatches a click at it. If the layout shifts in between — a
 *       product image finishing its download and reflowing the grid, a menu
 *       still sliding — the click lands where the element used to be. This is
 *       why a suite passes on a developer machine with warm caches and fails on
 *       a cold CI runner: the same tests, a slower network, a different result.
 *   <li><b>Would a click actually reach it?</b> An element can be displayed,
 *       enabled and stationary while sitting outside the viewport or underneath
 *       an overlay. {@code document.elementFromPoint} is the browser's own hit
 *       test, and it is the question a real click asks.
 * </ol>
 *
 * <p>Playwright performs both checks internally, which is precisely why the
 * companion TypeScript suite never needed them.
 */
public final class Actionability {

    /** Geometry and hit-test result for one poll, gathered in a single round trip. */
    private static final String PROBE_SCRIPT = """
            const el = arguments[0];
            const rect = el.getBoundingClientRect();

            if (rect.width === 0 || rect.height === 0) {
                return { hittable: false, x: 0, y: 0, w: 0, h: 0 };
            }

            const x = rect.left + rect.width / 2;
            const y = rect.top + rect.height / 2;

            const inViewport = x >= 0 && y >= 0
                    && x <= window.innerWidth && y <= window.innerHeight;

            let hit = false;
            if (inViewport) {
                const atPoint = document.elementFromPoint(x, y);
                hit = atPoint !== null
                        && (atPoint === el || el.contains(atPoint) || atPoint.contains(el));
            }

            return {
                hittable: hit,
                x: Math.round(rect.left * 100) / 100,
                y: Math.round(rect.top * 100) / 100,
                w: Math.round(rect.width * 100) / 100,
                h: Math.round(rect.height * 100) / 100
            };
            """;

    private Actionability() {
        // Utility class.
    }

    /**
     * Waits until an element is hit-testable at its centre <em>and</em> has
     * occupied the same box for two consecutive polls.
     *
     * <p>Two polls rather than one: a single sample cannot distinguish an
     * element at rest from one that happens to be passing through. The
     * {@link org.openqa.selenium.support.ui.WebDriverWait} polling interval
     * supplies the gap between samples, so no sleep is needed.
     */
    public static ExpectedCondition<Boolean> settledAndHittable(By locator) {
        // Holds the previous poll's geometry; null until the first sample.
        final String[] previous = {null};

        return new ExpectedCondition<>() {
            @Override
            public Boolean apply(WebDriver driver) {
                List<WebElement> found = driver.findElements(locator);

                if (found.isEmpty()) {
                    previous[0] = null;
                    return false;
                }

                Map<?, ?> probe = probe(driver, found.get(0));

                if (!Boolean.TRUE.equals(probe.get("hittable"))) {
                    previous[0] = null;
                    return false;
                }

                String geometry = probe.get("x") + "," + probe.get("y")
                        + "," + probe.get("w") + "," + probe.get("h");

                boolean settled = geometry.equals(previous[0]);
                previous[0] = geometry;

                return settled;
            }

            @Override
            public String toString() {
                return "element located by " + locator
                        + " to stop moving and be hit-testable at its centre";
            }
        };
    }

    private static Map<?, ?> probe(WebDriver driver, WebElement element) {
        Object result = ((JavascriptExecutor) driver).executeScript(PROBE_SCRIPT, element);

        return result instanceof Map<?, ?> map ? map : Map.of("hittable", false);
    }
}
