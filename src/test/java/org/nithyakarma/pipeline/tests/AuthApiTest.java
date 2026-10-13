package org.nithyakarma.pipeline.tests;

import io.restassured.response.Response;
import org.nithyakarma.pipeline.api.SupabaseApi;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// T4: no browser. The captcha is enforced by the server, not only by the UI.
public class AuthApiTest {

    @Test(groups = "public")
    public void passwordLoginWithoutCaptchaTokenIsRejected() {
        Response response = SupabaseApi.anon()
                .queryParam("grant_type", "password")
                .body(Map.of("email", "nobody@nithyakarma.test", "password", "not-a-real-password"))
                .post("/auth/v1/token");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("error_code")).isEqualTo("captcha_failed");
    }
}
