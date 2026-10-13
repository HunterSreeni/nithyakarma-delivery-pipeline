package org.nithyakarma.pipeline.pages;

import org.nithyakarma.pipeline.bidi.NetworkWaits;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ProfilePage extends BasePage {
    // the app paints this cached profile first and the server copy ~0.3s later
    private static final String CACHE_KEY = "nk_profile_cache_v1";

    private final By displayNameInput = By.id("pf-name");
    private final By leaderboardOptIn = By.xpath("//label[contains(., 'Show me on community leaderboards')]/input");

    public ProfilePage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "/profile");
        freshLoad();
    }

    // drop the cache, reload, and wait until the app has rewritten it from the server
    public void freshLoad() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("localStorage.removeItem(arguments[0]);", CACHE_KEY);
        driver.navigate().refresh();
        wait.until(d -> js.executeScript("return localStorage.getItem(arguments[0]);", CACHE_KEY) != null);
        visible(displayNameInput);
    }

    public boolean isLeaderboardOptedIn() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(leaderboardOptIn)).isSelected();
    }

    // clicks only if needed, and waits (BiDi) for the PATCH /profiles save to succeed
    public void setLeaderboardOptIn(boolean wanted) {
        WebElement box = wait.until(ExpectedConditions.presenceOfElementLocated(leaderboardOptIn));
        if (box.isSelected() != wanted) {
            NetworkWaits.clickAndWaitFor(driver, "PATCH", "/rest/v1/profiles", box::click);
        }
        wait.until(d -> isLeaderboardOptedIn() == wanted);
    }
}
