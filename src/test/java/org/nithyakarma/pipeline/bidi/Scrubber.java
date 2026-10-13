package org.nithyakarma.pipeline.bidi;

import java.util.List;
import java.util.regex.Pattern;

// Redacts secrets from browser console/network captures before they are written to reports.
// Raw captures stay in memory; only scrub() output ever reaches a file.
public final class Scrubber {

    private record Rule(Pattern pattern, String replacement) {
    }

    private static final List<Rule> RULES = List.of(
            // JWTs (access tokens, the anon/service JWT keys): three base64url parts starting eyJ
            new Rule(Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "[REDACTED_JWT]"),
            // new-style Supabase keys
            new Rule(Pattern.compile("sb_(secret|publishable)_[A-Za-z0-9_-]+"), "[REDACTED_KEY]"),
            // header values: apikey: xxx / Authorization: Bearer xxx
            new Rule(Pattern.compile("(?i)(apikey|authorization)([\"']?\\s*[:=]\\s*[\"']?)(bearer\\s+)?[^\\s\"',}]+"),
                    "$1$2[REDACTED]"),
            // JSON fields holding tokens
            new Rule(Pattern.compile("(?i)(\"(?:access_token|refresh_token|token_hash|hashed_token|provider_token)\"\\s*:\\s*\")[^\"]+"),
                    "$1[REDACTED]"),
            // tokens in URLs (magic links, OAuth redirects)
            new Rule(Pattern.compile("(?i)([?&#](?:token|token_hash|access_token|refresh_token|code)=)[^&\\s\"']+"),
                    "$1[REDACTED]")
    );

    private Scrubber() {
    }

    public static String scrub(String raw) {
        if (raw == null) {
            return "";
        }
        String out = raw;
        for (Rule rule : RULES) {
            out = rule.pattern().matcher(out).replaceAll(rule.replacement());
        }
        return out;
    }
}
