package org.nithyakarma.pipeline.tests;

import org.nithyakarma.pipeline.api.Cleanup;
import org.nithyakarma.pipeline.api.SupabaseApi;
import org.nithyakarma.pipeline.base.LoggedInUiTestBase;
import org.nithyakarma.pipeline.config.Config;
import org.nithyakarma.pipeline.pages.ProfilePage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// T9: flip the leaderboard toggle, prove it survived a reload AND reached the database, flip it back
public class LeaderboardTest extends LoggedInUiTestBase {

    @Test(groups = "features")
    public void leaderboardToggleIsSavedAndRestored() {
        if (!Config.allowDataWrites()) {
            throw new SkipException("writes are off (set E2E_ALLOW_DATA_WRITES=true)");
        }
        ProfilePage profile = new ProfilePage(driver);
        profile.open(baseUrl);
        boolean original = profile.isLeaderboardOptedIn();

        try {
            profile.setLeaderboardOptIn(!original);
            profile.freshLoad();
            assertThat(profile.isLeaderboardOptedIn()).as("after reload").isEqualTo(!original);
            assertThat(optInInDatabase()).as("database, read as A").isEqualTo(!original);

            profile.setLeaderboardOptIn(original);
            profile.freshLoad();
            assertThat(profile.isLeaderboardOptedIn()).as("switched back").isEqualTo(original);
        } finally {
            // safety net if the UI part failed midway; the toggle is A's own setting, so A may restore it
            Cleanup.restore(userA, "profiles", "id", userA.userId(), Map.of("leaderboard_opt_in", original));
        }
    }

    private boolean optInInDatabase() {
        return SupabaseApi.asUser(userA)
                .queryParam("id", "eq." + userA.userId())
                .queryParam("select", "leaderboard_opt_in")
                .get("/rest/v1/profiles")
                .jsonPath().getBoolean("[0].leaderboard_opt_in");
    }
}
