package org.nithyakarma.pipeline.driver;

import org.nithyakarma.pipeline.config.Config;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

// One place that decides local vs remote Chrome. Tests never call new ChromeDriver() themselves.
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        ChromeOptions options = new ChromeOptions();
        if (Config.headless()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1280,720");
        // WebDriver BiDi (webSocketUrl=true): network + console events over a websocket
        options.enableBiDi();
        // BiDi sessions auto-dismiss alerts by default; let tests handle them
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.IGNORE);

        String remoteUrl = Config.remoteUrl();
        if (remoteUrl.isEmpty()) {
            ChromeDriver driver = new ChromeDriver(options);
            // the app shows IST dates. CDP is Chrome-only and local-only;
            // remote Chrome gets TZ=Asia/Kolkata on its container instead
            driver.executeCdpCommand("Emulation.setTimezoneOverride", Map.of("timezoneId", "Asia/Kolkata"));
            return driver;
        }
        try {
            // Augmenter adds BiDi support to a plain RemoteWebDriver
            return new Augmenter().augment(new RemoteWebDriver(URI.create(remoteUrl).toURL(), options));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("SELENIUM_REMOTE_URL is not a valid URL", e);
        }
    }
}
