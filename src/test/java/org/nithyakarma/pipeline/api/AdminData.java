package org.nithyakarma.pipeline.api;

import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

// Service-role READS for setup and verification only: snapshot a row before a write test,
// prove data exists before an RLS "can't see it" check, confirm "really unchanged".
// The service role is read-only on the app's tables (the app revoked its UPDATE/DELETE),
// so cleanup goes through Cleanup, as the data's owner.
public final class AdminData {

    private AdminData() {
    }

    // one row as a map, e.g. row("profiles", "id", userId, "punya,ad_free_until")
    public static Map<String, Object> row(String table, String idColumn, String id, String columns) {
        Response response = SupabaseApi.serviceRole()
                .queryParam(idColumn, "eq." + id)
                .queryParam("select", columns)
                .get("/rest/v1/" + table);
        expect2xx(response, "read " + table);
        List<Map<String, Object>> rows = response.jsonPath().getList("$");
        if (rows.size() != 1) {
            throw new IllegalStateException("expected 1 " + table + " row, got " + rows.size());
        }
        return rows.get(0);
    }

    public static int count(String table, String column, String value) {
        Response response = SupabaseApi.serviceRole()
                .queryParam(column, "eq." + value)
                .queryParam("select", "id")
                .get("/rest/v1/" + table);
        expect2xx(response, "count " + table);
        return response.jsonPath().getList("$").size();
    }

    static void expect2xx(Response response, String what) {
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException(what + " failed: HTTP " + response.statusCode());
        }
    }
}
