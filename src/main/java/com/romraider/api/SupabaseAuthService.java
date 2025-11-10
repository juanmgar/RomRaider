package com.romraider.api;

import com.romraider.config.SecretsLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.Scanner;

public class SupabaseAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthService.class);

    private static String accessToken;
    private static String userId;
    private static String currentUserEmail;

    private static final String SUPABASE_URL = SecretsLoader.getSupabaseUrl();
    private static final String SUPABASE_KEY = SecretsLoader.getSupabaseKey();

    public static boolean login(String email, String password) {
        logger.info("Attempting login for: {}", email);

        JSONObject body = new JSONObject()
                .put("email", email)
                .put("password", password);

        JSONObject response = sendSupabaseRequest("/auth/v1/token?grant_type=password", body);

        if (response != null && response.has("access_token")) {
            accessToken = response.getString("access_token");
            userId = extractUserIdFromToken(accessToken);
            currentUserEmail = email;
            logger.info("Login successful. User ID: {}", userId);
            return true;
        }

        logger.warn("Login failed for {}", email);
        return false;
    }

    public static boolean register(String email, String password) {
        logger.info("Attempting registration for: {}", email);

        JSONObject body = new JSONObject()
                .put("email", email)
                .put("password", password);

        JSONObject response = sendSupabaseRequest("/auth/v1/signup", body);

        if (response != null) {
            logger.info("User registered successfully: {}", email);
            return true;
        }

        logger.warn("Registration failed for {}", email);
        return false;
    }

    public static void logout() {
        accessToken = null;
        userId = null;
        currentUserEmail = null;
        logger.info("Session cleared. Logged out.");
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static String getUserId() {
        return userId;
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    private static JSONObject sendSupabaseRequest(String endpoint, JSONObject body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(SUPABASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", SUPABASE_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Send JSON
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            int status = conn.getResponseCode();
            boolean success = status == 200 || status == 201;

            Scanner scanner = new Scanner(success ? conn.getInputStream() : conn.getErrorStream())
                    .useDelimiter("\\A");
            String responseBody = scanner.hasNext() ? scanner.next() : "";

            if (success) {
                return new JSONObject(responseBody.isEmpty() ? "{}" : responseBody);
            } else {
                logger.warn("Supabase request failed ({}): {}", status, responseBody);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error in Supabase request to {}", endpoint, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject payload = new JSONObject(payloadJson);
            return payload.optString("sub", null);
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token", e);
            return null;
        }
    }
}
