package org.nithyakarma.pipeline.tests;

import io.restassured.response.Response;
import org.nithyakarma.pipeline.api.AdminData;
import org.nithyakarma.pipeline.api.Cleanup;
import org.nithyakarma.pipeline.api.SupabaseApi;
import org.nithyakarma.pipeline.base.LoggedInUiTestBase;
import org.nithyakarma.pipeline.config.Config;
import org.nithyakarma.pipeline.pages.TodayPage;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// T8: mark the Morning sandhya in the UI, wait for the save with BiDi, check the row via the API as A,
// then put everything back. Marking a slot changes more than one row (the RPC submit_practice_log
// also updates punya, streaks and freeze credits), so cleanup restores all of them, as A.
public class SandhyaTest extends LoggedInUiTestBase {
    private static final String PROFILE_FIELDS = "punya,current_streak,best_streak,last_complete_date,freeze_credits";
    private static final String PRACTICE_FIELDS = "current_streak,best_streak,last_log_date,sequence_position";

    @Test(groups = "features")
    public void markingMorningSandhyaSavesOneLog() {
        if (!Config.allowDataWrites()) {
            throw new SkipException("writes are off (set E2E_ALLOW_DATA_WRITES=true)");
        }
        TodayPage today = new TodayPage(driver);
        today.open(baseUrl);
        if (today.sandhyasDone() > 0) {
            throw new SkipException("a sandhya slot is already marked today; not touching real data");
        }

        String sandhyaPracticeId = ownSandhyaPracticeId();
        Map<String, Object> profileBefore = AdminData.row("profiles", "id", userA.userId(), PROFILE_FIELDS);
        Map<String, Object> practiceBefore = AdminData.row("user_practices", "id", sandhyaPracticeId, PRACTICE_FIELDS);
        int streakEventsBefore = AdminData.count("streak_events", "owner_id", userA.userId());
        // a minute of slack for clock differences between this machine and the database
        String start = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS).toString();

        try {
            assertThat(today.markMorningSandhya()).as("default Gayatri count").isEqualTo("108");
            assertThat(today.slotButton("Morning").getDomAttribute("class")).contains("done");

            // the UI says saved; the database must agree (read as A, through RLS)
            List<Map<String, Object>> logs = SupabaseApi.asUser(userA)
                    .queryParam("user_practice_id", "eq." + sandhyaPracticeId)
                    .queryParam("created_at", "gte." + start)
                    .queryParam("select", "slot,count,owner_id")
                    .get("/rest/v1/practice_logs")
                    .jsonPath().getList("$");
            assertThat(logs).hasSize(1);
            assertThat(logs.get(0)).containsEntry("slot", "morning")
                    .containsEntry("count", 108)
                    .containsEntry("owner_id", userA.userId());
        } finally {
            // as A. NOTE: the profile restore works only while the T15 bug exists (users can edit
            // their own punya/streak). After the app fix it is refused and this fails loudly.
            Cleanup.deleteCreatedSince(userA, "practice_logs", start);
            Cleanup.restore(userA, "user_practices", "id", sandhyaPracticeId, practiceBefore);
            Cleanup.restore(userA, "profiles", "id", userA.userId(), profileBefore);
            // users cannot delete streak events (no policy), so a new one needs a manual reset
            if (AdminData.count("streak_events", "owner_id", userA.userId()) != streakEventsBefore) {
                throw new IllegalStateException("a streak event was created - manual reset needed, see TEST-DATA.md");
            }
        }
    }

    // A's own (not a child's) Sandhyavandhanam tracking row
    private String ownSandhyaPracticeId() {
        List<Integer> sandhyaIds = SupabaseApi.asUser(userA)
                .queryParam("is_sandhyavandhanam", "eq.true")
                .queryParam("select", "id")
                .get("/rest/v1/practices")
                .jsonPath().getList("id");
        Response rows = SupabaseApi.asUser(userA)
                .queryParam("owner_id", "eq." + userA.userId())
                .queryParam("family_member_id", "is.null")
                .queryParam("practice_id", "in.(" + String.join(",", sandhyaIds.stream().map(String::valueOf).toList()) + ")")
                .queryParam("select", "id")
                .get("/rest/v1/user_practices");
        List<String> ids = rows.jsonPath().getList("id");
        if (ids.size() != 1) {
            throw new SkipException("A must track Sandhyavandhanam for themselves exactly once, found " + ids.size());
        }
        return ids.get(0);
    }
}
