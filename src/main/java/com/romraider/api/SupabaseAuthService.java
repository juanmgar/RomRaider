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

/**
 * Servicio encargado de gestionar la autenticación con Supabase:
 * login, registro, cierre de sesión y restauración de sesión mediante token.
 *
 * Las respuestas visibles al usuario se gestionan desde los controladores.
 * Aquí solo se gestiona lógica interna y logs (en español).
 */
public class SupabaseAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthService.class);

    private static String accessToken;
    private static String userId;
    private static String currentUserEmail;

    private static final String SUPABASE_URL = SecretsLoader.getSupabaseUrl();
    private static final String SUPABASE_KEY = SecretsLoader.getSupabaseKey();

    /**
     * Realiza login mediante email y contraseña.
     *
     * @return true si el login es correcto, false en caso contrario.
     */
    public static boolean login(String email, String password) {
        logger.info("Intentando iniciar sesión con el usuario: {}", email);

        JSONObject body = new JSONObject()
                .put(APIsConstants.EMAIL, email)
                .put(APIsConstants.PASSWORD, password);

        JSONObject response = sendSupabaseRequest(
                "/auth/v1/token?grant_type=password",
                body
        );

        if (response != null && response.has(APIsConstants.ACCESS_TOKEN)) {
            accessToken = response.getString(APIsConstants.ACCESS_TOKEN);
            userId = extractUserIdFromToken(accessToken);
            currentUserEmail = email;

            logger.info("Inicio de sesión correcto. UserID detectado: {}", userId);
            return true;
        }

        logger.warn("Inicio de sesión fallido para {}", email);
        return false;
    }

    /**
     * Registra un usuario en Supabase.
     */
    public static boolean register(String email, String password) {
        logger.info("Intentando registrar usuario: {}", email);

        JSONObject body = new JSONObject()
                .put(APIsConstants.EMAIL, email)
                .put(APIsConstants.PASSWORD, password);

        JSONObject response = sendSupabaseRequest("/auth/v1/signup", body);

        if (response != null) {
            logger.info("Usuario registrado correctamente: {}", email);
            return true;
        }

        logger.warn("Fallo al registrar el usuario {}", email);
        return false;
    }

    /**
     * Limpia completamente la sesión local.
     */
    public static void logout() {
        accessToken = null;
        userId = null;
        currentUserEmail = null;

        logger.info("Sesión cerrada. Variables internas limpiadas.");
    }

    /**
     * Intenta restaurar la sesión desde un token guardado previamente.
     * Si el token sigue siendo válido, recupera el email del usuario desde Supabase.
     */
    public static boolean restoreSession(String token) {
        if (token == null || token.isBlank()) {
            logger.warn("No se puede restaurar la sesión: token vacío o nulo");
            return false;
        }

        try {
            accessToken = token;
            userId = extractUserIdFromToken(token);

            if (userId == null) {
                logger.warn("No se pudo extraer el UserID desde el token almacenado");
                return false;
            }

            // Consultamos la API para obtener el email del usuario
            URL url = new URL(SUPABASE_URL + "/auth/v1/user");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(APIsConstants.GET);
            conn.setRequestProperty(APIsConstants.APIKEY, SUPABASE_KEY);
            conn.setRequestProperty(APIsConstants.AUTHORIZATION,
                    APIsConstants.BEARER_PREFIX + token);

            int status = conn.getResponseCode();

            if (status == 200) {
                String body = new String(conn.getInputStream().readAllBytes());
                JSONObject json = new JSONObject(body);

                currentUserEmail = json.optString(APIsConstants.EMAIL, "(unknown)");

                logger.info("Sesión restaurada correctamente. UserID: {}, Email: {}",
                        userId, currentUserEmail);

                return true;
            } else {
                logger.warn("Restauración fallida. Supabase devolvió HTTP {}", status);
                currentUserEmail = "(unknown)";
                return true; // No invalidamos el token, solo avisamos
            }

        } catch (Exception e) {
            logger.error("Error restaurando la sesión desde token", e);
            return false;
        }
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

    /**
     * Envía una petición POST a Supabase con un body JSON.
     * Devuelve el JSON recibido o null en caso de error.
     */
    private static JSONObject sendSupabaseRequest(String endpoint, JSONObject body) {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(SUPABASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod(APIsConstants.POST);
            conn.setRequestProperty(APIsConstants.APIKEY, SUPABASE_KEY);
            conn.setRequestProperty(APIsConstants.AUTHORIZATION,
                    APIsConstants.BEARER_PREFIX + accessToken);
            conn.setRequestProperty(APIsConstants.CONTENT_TYPE, APIsConstants.CONTENT_TYPE_JSON);
            conn.setDoOutput(true);

            // Envío del JSON al servidor
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes());
            }

            int status = conn.getResponseCode();
            boolean success = status == 200 || status == 201;

            Scanner scanner = new Scanner(
                    success ? conn.getInputStream() : conn.getErrorStream()
            ).useDelimiter("\\A");

            String responseBody = scanner.hasNext() ? scanner.next() : "";

            if (success) {
                return new JSONObject(responseBody.isEmpty() ? "{}" : responseBody);
            } else {
                logger.warn("Petición a Supabase fallida (HTTP {}): {}", status, responseBody);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error en la petición Supabase a {}", endpoint, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Extrae el ID de usuario desde el payload de un token JWT.
     */
    private static String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            // El payload del JWT es Base64URL
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject payload = new JSONObject(payloadJson);

            return payload.optString(APIsConstants.SUB, null);

        } catch (Exception e) {
            logger.error("No se pudo extraer el userId desde el token JWT", e);
            return null;
        }
    }
}
