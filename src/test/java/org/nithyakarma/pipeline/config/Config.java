package org.nithyakarma.pipeline.config;

public final class Config {
    private Config() {
        // Private constructor to prevent instantiation
    }
    public static String baseUrl() {
        return required("BASE_URL");
    }
    public static String supabaseUrl() {
        return required("SUPABASE_URL");
    }

    public static String publishableKey() {
        return required("SUPABASE_PUBLISHABLE_KEY");
    }

    public static String serviceRoleKey() {
        return required("SUPABASE_SERVICE_ROLE_KEY");
    }

    public static String userAEmail() {
        return required("E2E_USER_A_EMAIL");
    }

    public static String userBEmail() {
        return required("E2E_USER_B_EMAIL");
    }

    public static boolean allowDataWrites() {
        return Boolean.parseBoolean(optional("E2E_ALLOW_DATA_WRITES", "false"));
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var " + name + " (see .env.example)");
        }
        return value.trim();
    }

    private static String optional(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
