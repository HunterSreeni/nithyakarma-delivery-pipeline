package org.nithyakarma.pipeline.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    // textContent, not getText(): getText() returns the CSS-uppercased screen text
    public static String text(WebElement element) {
        String value = element.getDomProperty("textContent");
        // &nbsp; is not whitespace to Java's trim(); the app uses it as a placeholder
        return value == null ? "" : value.replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }

    // waits for non-blank text: React renders the element first and fills it in later
    protected String textOf(By locator) {
        visible(locator);
        return wait.until(d -> {
            var found = d.findElements(locator);
            String value = found.isEmpty() ? "" : text(found.get(0));
            return value.isBlank() ? null : value;
        });
    }

    protected void waitForText(By locator, String regex) {
        wait.until(d -> {
            var found = d.findElements(locator);
            return !found.isEmpty() && text(found.get(0)).matches(regex);
        });
    }
}
