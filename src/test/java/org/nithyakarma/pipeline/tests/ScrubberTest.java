package org.nithyakarma.pipeline.tests;

import org.nithyakarma.pipeline.bidi.Scrubber;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Unit test, no browser: proves the scrubber removes every kind of secret it is meant to.
// All values below are fake.
public class ScrubberTest {
    private static final String FAKE_JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJlLWZha2U";

    @Test
    public void redactsJwtsKeysHeadersAndMagicLinks() {
        String raw = String.join("\n",
                "fetch failed Authorization: Bearer " + FAKE_JWT,
                "apikey: sb_publishable_FAKEfakeFAKE123",
                "{\"access_token\":\"" + FAKE_JWT + "\",\"refresh_token\":\"r3fr3sh-fake\"}",
                "https://x.supabase.co/auth/v1/verify?token=abc123fake&type=magiclink",
                "secret sb_secret_FAKEfake456 leaked");

        String clean = Scrubber.scrub(raw);

        assertThat(clean).doesNotContain(FAKE_JWT, "sb_publishable_FAKE", "sb_secret_FAKE", "r3fr3sh-fake", "abc123fake");
        assertThat(clean).contains("[REDACTED", "type=magiclink", "fetch failed");
    }

    @Test
    public void leavesNormalTextAlone() {
        String line = "INFO Namaskaram, 1 of 3 sandhyas done";
        assertThat(Scrubber.scrub(line)).isEqualTo(line);
    }
}
