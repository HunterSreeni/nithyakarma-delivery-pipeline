package org.nithyakarma.pipeline.tests;

import org.assertj.core.api.SoftAssertions;
import org.nithyakarma.pipeline.base.LoggedInUiTestBase;
import org.nithyakarma.pipeline.pages.AppHeader;
import org.nithyakarma.pipeline.pages.TodayPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

// T5-T7: logged in as A, read-only
public class DashboardTest extends LoggedInUiTestBase {
    private TodayPage today;

    @BeforeMethod
    public void openToday() {
        today = new TodayPage(driver);
        today.open(baseUrl);
    }

    // T5. The DAG demo breaks this on purpose (-Ddag.breakSession=true) to show downstream SKIPs.
    @Test(groups = "session")
    public void sessionLoadsDashboardWithTodayInIst() {
        if (Boolean.getBoolean("dag.breakSession")) {
            fail("dag.breakSession=true: session step broken on purpose for the DAG demo");
        }
        assertThat(today.dateLine()).isEqualTo(TodayPage.expectedDateLine());
        assertThat(today.greeting()).matches("Namaskaram, \\S+.*");
        assertThat(today.progressLine()).matches("\\d+ anushtanams? done today.*");
    }

    // T6
    @Test(groups = "features")
    public void everyTabNavigatesToItsPage() {
        AppHeader header = new AppHeader(driver);
        for (String name : AppHeader.TABS.keySet()) {
            header.goTo(name);
            assertThat(header.tab(name).getDomAttribute("aria-current")).as(name).isEqualTo("page");
        }
    }

    // T7
    @Test(groups = "features")
    public void panchangamShowsDayDetailsAndThreeKalams() {
        assertThat(today.panchangamText()).contains("Nakshatram", "Times shown in IST");
        Map<String, String> kalams = today.kalamTimes();
        assertThat(kalams).containsOnlyKeys("Rahu Kalam", "Yamagandam", "Gulika Kalam");
        SoftAssertions softly = new SoftAssertions();
        kalams.forEach((name, time) -> softly.assertThat(time).as(name).matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}"));
        softly.assertAll();
    }
}
