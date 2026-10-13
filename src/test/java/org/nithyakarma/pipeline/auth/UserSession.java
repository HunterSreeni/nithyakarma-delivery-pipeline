package org.nithyakarma.pipeline.auth;

// A logged-in user: id + JWT for API calls, raw JSON for the browser's localStorage.
// Held in memory only. Never logged, never written to a file.
public record UserSession(String userId, String accessToken, String json) {

    // keep tokens out of test reports if TestNG ever prints a parameter
    @Override
    public String toString() {
        return "UserSession[userId=" + userId + "]";
    }
}
