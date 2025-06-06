package com.romraider.api;

import com.romraider.utils.AppInitializer;
import com.romraider.utils.PropertyUtils;
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

    public static boolean login(String email, String password) {
        logger.info("Attempting login for: {}", email);

        try {
            PropertyUtils secrets = AppInitializer.loadSecrets();
            String supabaseURL = secrets.get("SUPABASE_URL");
            String supabaseAPIKey = secrets.get("SUPABASE_KEY");

            URL url = new URL(supabaseURL + "/auth/v1/token?grant_type=password");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", supabaseAPIKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String responseBody = scanner.hasNext() ? scanner.next() : "";
                JSONObject json = new JSONObject(responseBody);

                accessToken = json.getString("access_token");
                userId = extractUserIdFromToken(accessToken);
                currentUserEmail = email;

                logger.info("Login successful. User ID: {}", userId);
                return true;
            } else {
                logger.warn("Login failed. HTTP status: {}", status);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error connecting to Supabase during login", e);
            return false;
        }
    }

    private static String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject payload = new JSONObject(payloadJson);

            return payload.getString("sub");
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token", e);
            return null;
        }
    }

    public static boolean register(String email, String password) {
        logger.info("Attempting registration for: {}", email);

        try {
            PropertyUtils secrets = AppInitializer.loadSecrets();
            String supabaseURL = secrets.get("SUPABASE_URL");
            String supabaseAPIKey = secrets.get("SUPABASE_KEY");

            URL url = new URL(supabaseURL + "/auth/v1/signup");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("apikey", supabaseAPIKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            int status = conn.getResponseCode();
            if (status == 200 || status == 201) {
                logger.info("User registered successfully.");
                return true;
            } else {
                logger.warn("Registration failed. HTTP status: {}", status);
                return false;
            }

        } catch (Exception e) {
            logger.error("Error connecting to Supabase during registration", e);
            return false;
        }
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    public static String getUserId() {
        return userId;
    }

    public static void logout() {
        accessToken = null;
        userId = null;
        logger.info("Session cleared. Logged out.");
    }
}
