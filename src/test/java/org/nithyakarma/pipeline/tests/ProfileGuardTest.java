package org.nithyakarma.pipeline.tests;

import org.nithyakarma.pipeline.api.AdminData;
import org.nithyakarma.pipeline.api.Cleanup;
import org.nithyakarma.pipeline.api.SupabaseApi;
import org.nithyakarma.pipeline.auth.Sessions;
import org.nithyakarma.pipeline.auth.UserSession;
import org.nithyakarma.pipeline.config.Config;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// T15: a user must not be able to give themselves punya or ad-free time through the API.
// KNOWN BUG (found 2026-10-11): RLS only checks row ownership, and `authenticated` may UPDATE
// every column of its own profile. This test FAILS until the app is fixed, so it lives in the
// "known-bug" group, which the regression and DAG suites exclude.
// Run it alone: mvn test -Dsuite=testng-known-bugs.xml (with E2E_ALLOW_DATA_WRITES=true)
public class ProfileGuardTest {
    private static final String GUARDED = "punya,ad_free_until";

    @Test(groups = "known-bug")
    public void userCannotEditServerOwnedProfileFields() {
        if (!Config.allowDataWrites()) {
            throw new SkipException("writes are off (set E2E_ALLOW_DATA_WRITES=true)");
        }
        UserSession a = Sessions.userA();
        Map<String, Object> before = AdminData.row("profiles", "id", a.userId(), GUARDED);
        try {
            int punya = ((Number) before.get("punya")).intValue();
            SupabaseApi.asUser(a)
                    .queryParam("id", "eq." + a.userId())
                    .body(Map.of("punya", punya + 1000, "ad_free_until", "2099-12-31"))
                    .patch("/rest/v1/profiles");

            assertThat(AdminData.row("profiles", "id", a.userId(), GUARDED))
                    .as("server-owned fields after A's own PATCH").isEqualTo(before);
        } finally {
            // needed while the bug exists: the PATCH above really changes the row.
            // A can put it back for the same reason (once fixed, nothing changed, nothing to restore).
            if (!AdminData.row("profiles", "id", a.userId(), GUARDED).equals(before)) {
                Cleanup.restore(a, "profiles", "id", a.userId(), before);
            }
        }
    }
}
