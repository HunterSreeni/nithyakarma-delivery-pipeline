package org.nithyakarma.pipeline.api;

import io.restassured.response.Response;
import org.nithyakarma.pipeline.auth.UserSession;

import java.util.List;
import java.util.Map;

// Puts data back after a write test, AS THE USER WHO OWNS IT (RLS applies).
// If the app refuses a restore, fail loudly: silent leftovers on production data are worse than a red build.
public final class Cleanup {
    private static final String MANUAL = " - manual reset needed, see TEST-DATA.md";

    private Cleanup() {
    }

    // PATCH the owner's row back to its snapshot and check it really changed back
    public static void restore(UserSession owner, String table, String idColumn, String id, Map<String, Object> values) {
        Response response = SupabaseApi.asUser(owner)
                .header("Prefer", "return=representation")
                .queryParam(idColumn, "eq." + id)
                .body(values)
                .patch("/rest/v1/" + table);
        List<Object> rows = response.statusCode() / 100 == 2 ? response.jsonPath().getList("$") : List.of();
        if (rows.size() != 1 || !AdminData.row(table, idColumn, id, String.join(",", values.keySet())).equals(values)) {
            throw new IllegalStateException("restore " + table + " refused (HTTP " + response.statusCode() + ")" + MANUAL);
        }
    }

    // delete rows the owner created since the test started
    public static void deleteCreatedSince(UserSession owner, String table, String isoStart) {
        Response response = SupabaseApi.asUser(owner)
                .queryParam("owner_id", "eq." + owner.userId())
                .queryParam("created_at", "gte." + isoStart)
                .delete("/rest/v1/" + table);
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("clean up " + table + " failed: HTTP " + response.statusCode() + MANUAL);
        }
    }
}
