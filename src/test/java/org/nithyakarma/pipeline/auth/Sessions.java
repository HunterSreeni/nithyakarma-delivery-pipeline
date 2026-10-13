package org.nithyakarma.pipeline.auth;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.nithyakarma.pipeline.api.SupabaseApi;
import org.nithyakarma.pipeline.config.Config;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.util.Map;

// Login without the captcha (Turnstile blocks automated browsers):
// 1. the service-role key asks Supabase admin for a one-time magic-link token
// 2. /verify swaps that token for a real session, as the user
// 3. the session goes into localStorage, where the app's supabase-js looks for it
public final class Sessions {

    private Sessions() {
    }

    public static UserSession userA() {
        return create(Config.userAEmail());
    }

    public static UserSession userB() {
        return create(Config.userBEmail());
    }

    public static UserSession create(String email) {
        Response link = SupabaseApi.serviceRole()
                .body(Map.of("type", "magiclink", "email", email))
                .post("/auth/v1/admin/generate_link");
        if (link.statusCode() != 200) {
            // status only: the body can contain the magic link
            throw new IllegalStateException("generate_link failed: HTTP " + link.statusCode());
        }

        Response verify = SupabaseApi.anon()
                .body(Map.of("type", "magiclink", "token_hash", link.jsonPath().getString("hashed_token")))
                .post("/auth/v1/verify");
        if (verify.statusCode() != 200) {
            throw new IllegalStateException("verify failed: HTTP " + verify.statusCode());
        }

        String json = verify.asString();
        JsonPath body = new JsonPath(json);
        return new UserSession(body.getString("user.id"), body.getString("access_token"), json);
    }

    // open the app, plant the session, reload so the app picks it up
    public static void injectInto(WebDriver driver, UserSession session) {
        driver.get(Config.baseUrl());
        ((JavascriptExecutor) driver).executeScript(
                "localStorage.setItem(arguments[0], arguments[1]);", storageKey(), session.json());
        driver.navigate().refresh();
    }

    // supabase-js stores the session under sb-<project-ref>-auth-token
    public static String storageKey() {
        String projectRef = URI.create(Config.supabaseUrl()).getHost().split("\\.")[0];
        return "sb-" + projectRef + "-auth-token";
    }
}
