package org.nithyakarma.pipeline.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.LinkedHashMap;
import java.util.Map;

// top bar shared by every logged-in page
public class AppHeader extends BasePage {
    // tab name -> url path
    public static final Map<String, String> TABS = new LinkedHashMap<>();

    static {
        TABS.put("Today", "/");
        TABS.put("Learning", "/learning");
        TABS.put("History", "/history");
        TABS.put("Sabha", "/sabha");
        TABS.put("Calendar", "/calendar");
        TABS.put("Profile", "/profile");
    }

    private final By logoutButton = By.cssSelector("button.nav-logout");

    public AppHeader(WebDriver driver) {
        super(driver);
    }

    public WebElement tab(String name) {
        return visible(By.xpath("//nav[@aria-label='Primary']/a[normalize-space()='" + name + "']"));
    }

    // the header renders once the session has loaded
    public void waitUntilLoggedIn() {
        visible(logoutButton);
    }

    public boolean isLogoutVisible() {
        return driver.findElements(logoutButton).stream().anyMatch(WebElement::isDisplayed);
    }

    public void logout() {
        visible(logoutButton).click();
    }

    public void goTo(String name) {
        tab(name).click();
        String path = TABS.get(name);
        wait.until(ExpectedConditions.urlMatches(path.equals("/") ? "/$" : path + "$"));
        // aria-current moves one render after the url changes
        wait.until(d -> "page".equals(tab(name).getDomAttribute("aria-current")));
    }
}
