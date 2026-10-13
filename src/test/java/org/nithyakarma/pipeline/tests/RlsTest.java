package org.nithyakarma.pipeline.tests;

import io.restassured.response.Response;
import org.nithyakarma.pipeline.api.AdminData;
import org.nithyakarma.pipeline.api.Cleanup;
import org.nithyakarma.pipeline.api.SupabaseApi;
import org.nithyakarma.pipeline.auth.Sessions;
import org.nithyakarma.pipeline.auth.UserSession;
import org.nithyakarma.pipeline.config.Config;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// T11-T13: Row Level Security, through the same REST API the app uses, with A's own JWT.
// Every "A sees nothing of B" check first proves (service role) that B's data really exists,
// otherwise an empty result would pass even with RLS switched off.
// Expected responses were observed by hand first: see TEST-DATA.md.
public class RlsTest {
    private UserSession a;
    private UserSession b;

    @BeforeClass(alwaysRun = true)
    public void sessions() {
        a = Sessions.userA();
        b = Sessions.userB();
    }

    // T11
    @Test(groups = "features")
    public void userCannotReadAnotherUsersProfile() {
        assertThat(AdminData.count("profiles", "id", b.userId())).as("B's profile exists").isEqualTo(1);

        List<String> visible = SupabaseApi.asUser(a).queryParam("select", "id")
                .get("/rest/v1/profiles").jsonPath().getList("id");
        assertThat(visible).as("profiles A can see").containsExactly(a.userId());

        Response targeted = SupabaseApi.asUser(a).queryParam("id", "eq." + b.userId())
                .queryParam("select", "id").get("/rest/v1/profiles");
        assertThat(targeted.statusCode()).isEqualTo(200);
        assertThat(targeted.jsonPath().getList("$")).as("B's profile, asked for directly").isEmpty();
    }

    // T12 (attempts writes; they must all be refused)
    @Test(groups = "features")
    public void userCannotWriteAnotherUsersData() {
        if (!Config.allowDataWrites()) {
            throw new SkipException("writes are off (set E2E_ALLOW_DATA_WRITES=true)");
        }
        Map<String, Object> bBefore = AdminData.row("profiles", "id", b.userId(), "leaderboard_opt_in");
        boolean original = (Boolean) bBefore.get("leaderboard_opt_in");
        try {
            // update B's profile: RLS filters the row out, so 0 rows change and no error is raised
            Response update = SupabaseApi.asUser(a)
                    .header("Prefer", "return=representation")
                    .queryParam("id", "eq." + b.userId())
                    .body(Map.of("leaderboard_opt_in", !original))
                    .patch("/rest/v1/profiles");
            assertThat(update.statusCode()).isEqualTo(200);
            assertThat(update.jsonPath().getList("$")).as("rows updated").isEmpty();
            assertThat(AdminData.row("profiles", "id", b.userId(), "leaderboard_opt_in"))
                    .as("B unchanged").isEqualTo(bBefore);

            // add a child under B: the insert policy rejects it
            assertRejectedByRls(SupabaseApi.asUser(a)
                    .body(Map.of("parent_id", b.userId()))
                    .post("/rest/v1/family_members"));

            // write a practice log directly (only the submit_practice_log RPC may): no insert policy
            assertRejectedByRls(SupabaseApi.asUser(a)
                    .body(Map.of("owner_id", a.userId()))
                    .post("/rest/v1/practice_logs"));
        } finally {
            // only matters if RLS were broken and A's update went through; B puts their own row back
            if (!AdminData.row("profiles", "id", b.userId(), "leaderboard_opt_in").equals(bBefore)) {
                Cleanup.restore(b, "profiles", "id", b.userId(), bBefore);
            }
        }
    }

    // T13
    @Test(groups = "features")
    public void userSeesOnlyOwnChildren() {
        assertThat(AdminData.count("family_members", "parent_id", b.userId())).as("B has children").isPositive();

        List<String> parents = SupabaseApi.asUser(a).queryParam("select", "parent_id")
                .get("/rest/v1/family_members").jsonPath().getList("parent_id");
        assertThat(parents).as("children A can see").isNotEmpty().allMatch(a.userId()::equals);

        List<Object> bChildren = SupabaseApi.asUser(a).queryParam("parent_id", "eq." + b.userId())
                .queryParam("select", "id").get("/rest/v1/family_members").jsonPath().getList("$");
        assertThat(bChildren).as("B's children, asked for directly").isEmpty();
    }

    private static void assertRejectedByRls(Response response) {
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.jsonPath().getString("code")).isEqualTo("42501");
    }
}
