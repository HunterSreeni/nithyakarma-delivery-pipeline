package org.nithyakarma.pipeline.api;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.nithyakarma.pipeline.auth.UserSession;
import org.nithyakarma.pipeline.config.Config;

import static io.restassured.RestAssured.given;

// Shared REST Assured starting points. No request/response logging on purpose:
// headers carry the apikey and JWTs.
public final class SupabaseApi {

    private SupabaseApi() {
    }

    // anonymous: publishable key only, RLS treats the caller as logged out
    public static RequestSpecification anon() {
        return given()
                .baseUri(Config.supabaseUrl())
                .header("apikey", Config.publishableKey())
                .contentType(ContentType.JSON);
    }

    // as a real user: RLS applies with auth.uid() = that user. Use this for every assertion.
    public static RequestSpecification asUser(UserSession session) {
        return anon().header("Authorization", "Bearer " + session.accessToken());
    }

    // bypasses RLS. Setup and cleanup ONLY (mint sessions, snapshot and restore state).
    public static RequestSpecification serviceRole() {
        String key = Config.serviceRoleKey();
        return given()
                .baseUri(Config.supabaseUrl())
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .contentType(ContentType.JSON);
    }
}
