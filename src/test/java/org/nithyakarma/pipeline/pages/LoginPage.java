package org.nithyakarma.pipeline.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginPage extends BasePage {
    private final By logo = By.cssSelector("img[alt='Nithyakarma']");
    private final By emailInput = By.id("auth-email");
    private final By passwordInput = By.id("auth-password");
    private final By heading = By.xpath("//h1[normalize-space()='Welcome back']");
    private final By captchaWidget = By.cssSelector("div.turnstile-widget");
    // the submit button reads "Verifying..." until Turnstile passes, then "Sign In"
    private final By verifyingButton = By.xpath("//button[normalize-space()='Verifying...']");
    private final By signInButton = By.xpath("//button[normalize-space()='Sign In']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl);
        visible(logo);
    }

    public boolean isShown() {
        try {
            visible(heading);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // description -> locator, for a soft-assert loop in the test
    public Map<String, By> expectedElements() {
        Map<String, By> elements = new LinkedHashMap<>();
        elements.put("welcome heading", heading);
        elements.put("hero image", By.cssSelector("img[alt='Periyava']"));
        elements.put("email field", emailInput);
        elements.put("password field", passwordInput);
        elements.put("captcha widget", captchaWidget);
        elements.put("Google SSO", By.xpath("//button[contains(normalize-space(), 'Continue with Google')]"));
        elements.put("forgot password", By.xpath("//button[normalize-space()='Forgot password?']"));
        elements.put("submit (Verifying...)", verifyingButton);
        elements.put("create account", By.xpath("//button[contains(normalize-space(), 'Create account')]"));
        elements.put("terms text", By.cssSelector("div.auth-agree"));
        return elements;
    }

    public boolean isDisplayed(By locator) {
        return driver.findElements(locator).stream().anyMatch(WebElement::isDisplayed);
    }

    public void fill(String field, String value) {
        driver.findElement(field.equals("email") ? emailInput : passwordInput).sendKeys(value);
    }

    // the browser's own HTML5 validation state for the field
    public String validity(String field) {
        WebElement el = driver.findElement(field.equals("email") ? emailInput : passwordInput);
        return (String) ((JavascriptExecutor) driver).executeScript(
                "const v = arguments[0].validity;"
                        + "if (v.valueMissing) return 'valueMissing';"
                        + "if (v.typeMismatch) return 'typeMismatch';"
                        + "if (v.tooShort) return 'tooShort';"
                        + "return v.valid ? 'valid' : 'other';", el);
    }

    // true if Turnstile let this browser through within the timeout (it should not)
    public boolean captchaPasses(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(signInButton));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isVerifyingShown() {
        return isDisplayed(verifyingButton);
    }
}
