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
 * Servicio encargado de gestionar la autenticación con Supabase.
 *
 * <p>Funciones principales:</p>
 * <ul>
 *     <li>Inicio de sesión mediante email/contraseña</li>
 *     <li>Registro de usuarios</li>
 *     <li>Cierre de sesión y limpieza de tokens locales</li>
 *     <li>Restauración de sesión mediante refresh token</li>
 *     <li>Extracción del userId a partir del JWT</li>
 *     <li>Renovación del accessToken cuando expira</li>
 * </ul>
 *
 * <p>La interacción con Supabase Auth se realiza mediante peticiones HTTP
 * a los endpoints oficiales de autenticación.</p>
 *
 * <p>Las respuestas visibles al usuario final se gestionan desde los controladores;
 * este servicio solo contiene lógica interna y logs.</p>
 */
public class SupabaseAuthService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthService.class);

    /** Token de acceso proporcionado por Supabase tras login. */
    private static String accessToken;

    /** Token de refresco utilizado para obtener un nuevo accessToken cuando expira. */
    private static String refreshToken;

    /** ID del usuario autenticado (extraído del JWT). */
    private static String userId;

    /** Email del usuario actual. */
    private static String currentUserEmail;

    /** URL base del proyecto Supabase. */
    private static final String SUPABASE_URL = SecretsLoader.getSupabaseUrl();

    /** API key de autenticación asociada al proyecto. */
    private static final String SUPABASE_KEY = SecretsLoader.getSupabaseKey();

    /**
     * Realiza login mediante email y contraseña contra Supabase Auth.
     *
     * @param email email del usuario
     * @param password contraseña del usuario
     * @return true si la autenticación fue correcta; false en caso contrario
     *
     * <p>Si el login tiene éxito:</p>
     * <ul>
     *     <li>accessToken y refreshToken se almacenan</li>
     *     <li>se extrae el userId del JWT devuelto</li>
     * </ul>
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
     * Registra un usuario en Supabase Auth.
     *
     * @param email email del nuevo usuario
     * @param password contraseña del usuario
     * @return true si el usuario fue registrado correctamente; false en caso contrario
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
     * Cierra la sesión actual y elimina todos los tokens y datos del usuario en memoria.
     */
    public static void logout() {
        accessToken = null;
        refreshToken = null;
        userId = null;
        currentUserEmail = null;

        logger.info("Sesión cerrada. Variables internas limpiadas.");
    }

    /**
     * Intenta restaurar la sesión a partir de un refresh token previamente guardado.
     *
     * @param token refresh token almacenado localmente
     * @return true si la sesión pudo restaurarse; false si el token era inválido o expiró
     *
     * <p>Flujo:</p>
     * <ol>
     *     <li>Setea el refresh token recibido</li>
     *     <li>Intenta obtener un nuevo access token</li>
     *     <li>Extrae userId del JWT renovado</li>
     *     <li>Consulta el email del usuario mediante /auth/v1/user</li>
     * </ol>
     */
    public static boolean restoreSession(String token) {
        if (token == null || token.isBlank()) {
            logger.warn("No se puede restaurar la sesión: token vacío o nulo");
            return false;
        }

        try {
            refreshToken = token;

            // Intentar refrescar access token
            if (!refreshAccessToken()) {
                logger.warn("No se pudo refrescar el access token. Sesión expirada.");
                return false;
            }

            userId = extractUserIdFromToken(accessToken);

            if (userId == null) {
                logger.warn("No se pudo extraer el UserID desde el token almacenado");
                return false;
            }

            // Consultar el email del usuario mediante Supabase
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

    /** @return accessToken actual o null si no existe */
    public static String getAccessToken() {
        return accessToken;
    }

    /** @return refreshToken actual o null si no existe */
    public static String getRefreshToken() {
        return refreshToken;
    }

    /** @return ID del usuario autenticado o null */
    public static String getUserId() {
        return userId;
    }

    /** @return email del usuario autenticado o null */
    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }

    /**
     * Envía una petición POST a Supabase Auth con body JSON.
     *
     * @param endpoint ruta del endpoint (ej: "/auth/v1/signup")
     * @param body JSON a enviar en el cuerpo
     * @return objeto JSON recibido en la respuesta o null si hubo error
     *
     * <p>Gestiona internamente:</p>
     * <ul>
     *     <li>Cabeceras requeridas por Supabase</li>
     *     <li>Manejo de flujos de entrada/salida</li>
     *     <li>Logs en caso de error</li>
     * </ul>
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
     * Extrae el user_id del payload del JWT devuelto por Supabase Auth.
     *
     * @param token token JWT en formato estándar "header.payload.signature"
     * @return userId incluido en el campo "sub", o null si no puede extraerse
     */
    private static String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            // Decodificación Base64URL del payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JSONObject payload = new JSONObject(payloadJson);

            return payload.optString(APIsConstants.SUB, null);

        } catch (Exception e) {
            logger.error("No se pudo extraer el userId desde el token JWT", e);
            return null;
        }
    }

    /**
     * Renueva el accessToken utilizando el refreshToken actual.
     *
     * @return true si la renovación fue correcta; false si el token expiró o es inválido
     *
     * <p>Supabase devuelve un nuevo par (accessToken, refreshToken),
     * que reemplaza al anterior en memoria.</p>
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
