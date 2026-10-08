package com.deepakkhatri.qa.listeners;

import com.deepakkhatri.qa.driver.DriverManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

/**
 * Captures diagnostics at the moment of failure.
 *
 * <p>A listener rather than a try/catch in every test: the screenshot is taken
 * while the browser is still on the failing page, before any teardown has run.
 * Attaching it to Allure means the report shows what the page looked like when
 * the assertion failed, which is usually the whole investigation.
 *
 * <p>Nothing here is allowed to throw. A listener that fails while reporting a
 * failure replaces a useful assertion message with its own stack trace.
 */
public class TestListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        attachScreenshot("failure-" + result.getName());
        attachPageSource();
        attachContext(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Allure.addAttachment(
                "Skip reason",
                result.getThrowable() == null
                        ? "No throwable recorded"
                        : String.valueOf(result.getThrowable().getMessage()));
    }

    private void attachScreenshot(String name) {
        if (!DriverManager.isSet()) {
            return;
        }

        try {
            WebDriver driver = DriverManager.get();

            if (driver instanceof TakesScreenshot camera) {
                byte[] png = camera.getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");
            }
        } catch (RuntimeException e) {
            Allure.addAttachment("Screenshot capture failed", e.toString());
        }
    }

    private void attachPageSource() {
        if (!DriverManager.isSet()) {
            return;
        }

        try {
            Allure.addAttachment("Page source", "text/html",
                    DriverManager.get().getPageSource(), ".html");
        } catch (RuntimeException e) {
            Allure.addAttachment("Page source capture failed", e.toString());
        }
    }

    private void attachContext(ITestResult result) {
        if (!DriverManager.isSet()) {
            return;
        }

        try {
            String context = """
                    Test:    %s
                    URL:     %s
                    Thread:  %s
                    Message: %s
                    """.formatted(
                    result.getName(),
                    DriverManager.get().getCurrentUrl(),
                    Thread.currentThread().getName(),
                    result.getThrowable() == null ? "n/a" : result.getThrowable().getMessage());

            Allure.addAttachment("Failure context", context);
        } catch (RuntimeException e) {
            Allure.addAttachment("Context capture failed", e.toString());
        }
    }
}
