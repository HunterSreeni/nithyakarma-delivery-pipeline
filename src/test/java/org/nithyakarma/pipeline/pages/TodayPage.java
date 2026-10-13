package org.nithyakarma.pipeline.pages;

import org.nithyakarma.pipeline.bidi.NetworkWaits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TodayPage extends BasePage {
    private final By dateLine = By.cssSelector("main .eyebrow");
    private final By greeting = By.cssSelector("main h1.greet");
    private final By progressLine = By.cssSelector("main .greet-sub");
    // panchangam + kalams have no roles; the app's class names are the stable handle
    private final By panchangamBox = By.cssSelector(".panchangam-box");
    private final By kalams = By.cssSelector(".pb-kalam");
    private final By practiceCards = By.cssSelector(".practice-card");
    private final By sandhyaProgress = By.cssSelector(".sandhya-progress");
    // Morning asks for the Gayatri japam count before saving
    private final By countDialog = By.cssSelector("[role='dialog']");

    public TodayPage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl);
        visible(greeting);
    }

    // "Saturday, 03 October 2026" for today in IST
    public static String expectedDateLine() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH));
    }

    public String dateLine() {
        return textOf(dateLine);
    }

    public String greeting() {
        return textOf(greeting);
    }

    public String progressLine() {
        return textOf(progressLine);
    }

    public String panchangamText() {
        return textOf(panchangamBox);
    }

    // kalam name -> "HH:MM-HH:MM"
    public Map<String, String> kalamTimes() {
        visible(kalams);
        Map<String, String> times = new LinkedHashMap<>();
        for (WebElement kalam : driver.findElements(kalams)) {
            times.put(text(kalam.findElement(By.cssSelector(".pb-kalam-name"))),
                    text(kalam.findElement(By.cssSelector(".pb-kalam-time"))));
        }
        return times;
    }

    public int practiceCardCount() {
        visible(practiceCards);
        return driver.findElements(practiceCards).size();
    }

    public WebElement slotButton(String slot) {
        return visible(By.xpath("//button[contains(@class,'slot-btn') and normalize-space()='" + slot + "']"));
    }

    public String sandhyaProgress() {
        return textOf(sandhyaProgress);
    }

    public int sandhyasDone() {
        Matcher m = Pattern.compile("(\\d) of 3").matcher(sandhyaProgress());
        if (!m.find()) {
            throw new IllegalStateException("no sandhya progress text");
        }
        return Integer.parseInt(m.group(1));
    }

    // clicks Morning, saves the count dialog, and waits (BiDi) until the server accepted the log.
    // Returns the dialog's default count.
    public String markMorningSandhya() {
        slotButton("Morning").click();
        WebElement dialog = visible(countDialog);
        String defaultCount = dialog.findElement(By.cssSelector("input")).getDomProperty("value");
        WebElement save = dialog.findElement(By.xpath(".//button[normalize-space()='Save']"));
        NetworkWaits.clickAndWaitFor(driver, "POST", "/rest/v1/rpc/submit_practice_log", save::click);
        waitForText(sandhyaProgress, "1 of 3 sandhyas done.*");
        return defaultCount;
    }
}
