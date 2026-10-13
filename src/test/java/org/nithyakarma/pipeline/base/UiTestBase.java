package org.nithyakarma.pipeline.base;

import org.nithyakarma.pipeline.bidi.BrowserLogGuard;
import org.nithyakarma.pipeline.config.Config;
import org.nithyakarma.pipeline.driver.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

// One browser per test class (cheap enough, and classes never share state).
// A fresh BiDi log guard per test method; JsErrorListener reads it after each test.
@Listeners(JsErrorListener.class)
public abstract class UiTestBase {
    protected WebDriver driver;
    protected String baseUrl;
    BrowserLogGuard guard;

    @BeforeClass(alwaysRun = true)
    public void startBrowser() {
        baseUrl = Config.baseUrl();
        driver = DriverFactory.create();
    }

    @BeforeMethod(alwaysRun = true)
    public void startGuard() {
        guard = new BrowserLogGuard(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void stopGuard() {
        if (guard != null) {
            guard.close();
            guard = null;
        }
    }

    @AfterClass(alwaysRun = true)
    public void quitBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }
}
