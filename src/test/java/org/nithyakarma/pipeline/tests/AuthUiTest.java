package org.nithyakarma.pipeline.tests;

import org.assertj.core.api.SoftAssertions;
import org.nithyakarma.pipeline.base.UiTestBase;
import org.nithyakarma.pipeline.pages.LoginPage;
import org.openqa.selenium.By;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// T1-T3: the public login page, no session
public class AuthUiTest extends UiTestBase {
    private LoginPage login;

    @BeforeMethod
    public void openLogin() {
        login = new LoginPage(driver);
        login.open(baseUrl);
    }

    // T1
    @Test(groups = "public")
    public void loginPageRendersAllElements() {
        SoftAssertions softly = new SoftAssertions();
        for (Map.Entry<String, By> element : login.expectedElements().entrySet()) {
            softly.assertThat(login.isDisplayed(element.getValue())).as(element.getKey()).isTrue();
        }
        softly.assertAll();
    }

    @DataProvider(name = "fieldValidation")
    public Object[][] fieldValidation() {
        return new Object[][]{
                // case,                  field,      value,        expected browser validity
                {"bad email format", "email", "notanemail", "typeMismatch"},
                {"email missing domain", "email", "user@", "typeMismatch"},
                {"empty email", "email", "", "valueMissing"},
                {"valid email", "email", "a@b.co", "valid"},
                {"password 7 chars", "password", "1234567", "tooShort"},
                {"empty password", "password", "", "valueMissing"},
                {"password 8 chars", "password", "12345678", "valid"},
        };
    }

    // T2
    @Test(groups = "public", dataProvider = "fieldValidation")
    public void loginFieldValidation(String name, String field, String value, String expected) {
        login.fill(field, value);
        assertThat(login.validity(field)).as(name).isEqualTo(expected);
    }

    // T3: fake credentials on purpose; the captcha must stop an automated browser before any login
    @Test(groups = "public")
    public void captchaBlocksAutomatedLogin() {
        login.fill("email", "nobody@nithyakarma.test");
        login.fill("password", "not-a-real-password");
        assertThat(login.captchaPasses(Duration.ofSeconds(10))).as("Turnstile let the bot through").isFalse();
        assertThat(login.isVerifyingShown()).as("submit still says Verifying...").isTrue();
    }
}
