package com.romraider.api;

import com.romraider.config.SecretsLoader;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    private static String refreshToken;
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

            if (response.has(APIsConstants.REFRESH_TOKEN)) {
                refreshToken = response.getString(APIsConstants.REFRESH_TOKEN);
            }

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
        refreshToken = null;
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
            refreshToken = token;

            // intentar refrescar access token
            if (!refreshAccessToken()) {
                logger.warn("No se pudo refrescar el access token. Sesión expirada.");
                return false;
            }

            userId = extractUserIdFromToken(accessToken);

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
                    APIsConstants.BEARER_PREFIX + accessToken);

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
                return true;
            }

        } catch (Exception e) {
            logger.error("Error restaurando la sesión desde token", e);
            return false;
        }
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static String getRefreshToken() {
        return refreshToken;
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
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            boolean success = status == HttpURLConnection.HTTP_OK || status == HttpURLConnection.HTTP_CREATED;

            InputStream rawStream = success ? conn.getInputStream() : conn.getErrorStream();
            if (rawStream == null) {
                logger.warn("Petición a Supabase sin cuerpo de respuesta (HTTP {}).", status);
                return success ? new JSONObject() : null;
            }

            try (InputStream is = rawStream;
                 Scanner scanner = new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A")) {

                String responseBody = scanner.hasNext() ? scanner.next() : "";

                if (success) {
                    return new JSONObject(responseBody.isEmpty() ? "{}" : responseBody);
                } else {
                    logger.warn("Petición a Supabase fallida (HTTP {}): {}", status, responseBody);
                    return null;
                }
            }

        } catch (Exception e) {
            logger.error("Error en la petición Supabase a {}", endpoint, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
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

    /**
     * refresca el accessToken usando el refreshToken
     */
    private static boolean refreshAccessToken() {
        try {
            JSONObject body = new JSONObject()
                    .put(APIsConstants.REFRESH_TOKEN, refreshToken);

            JSONObject response = sendSupabaseRequest(
                    "/auth/v1/token?grant_type=refresh_token",
                    body
            );

            if (response == null || !response.has("access_token")) {
                return false;
            }

            accessToken = response.getString("access_token");

            if (response.has(APIsConstants.REFRESH_TOKEN)) {
                refreshToken = response.getString(APIsConstants.REFRESH_TOKEN);
            }

            logger.info("Access token refrescado correctamente.");
            return true;

        } catch (Exception e) {
            logger.error("Error refrescando el access token", e);
            return false;
        }
    }
}
