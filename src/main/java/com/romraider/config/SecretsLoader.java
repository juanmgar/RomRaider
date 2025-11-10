package com.romraider.config;

import java.io.InputStream;
import java.util.Properties;

public class SecretsLoader {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = SecretsLoader.class.getResourceAsStream("/config/secrets.properties")) {
            if (in != null) {
                props.load(in);
                System.out.println("[SecretsLoader] Loaded secrets.properties");
            } else {
                System.out.println("[SecretsLoader] No secrets.properties found, using environment variables only.");
            }
        } catch (Exception e) {
            System.err.println("[SecretsLoader] Error loading secrets.properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        // First, check environment variables (for CI/CD or SonarCloud)
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        // Otherwise, fall back to local secrets.properties
        return props.getProperty(key, "");
    }

    public static String getRawgApiKey() {
        return get("RAWG_API_KEY");
    }

    public static String getSupabaseUrl() {
        return get("SUPABASE_URL");
    }

    public static String getSupabaseKey() {
        return get("SUPABASE_KEY");
    }
}
