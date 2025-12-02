package com.romraider.auth;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gestiona la sesión persistida del usuario.
 *
 * <p>Se encarga de guardar, cargar y eliminar un refresh token
 * en el directorio del usuario ({@code ~/.romraider/session.json}).</p>
 *
 * <p>La idea es permitir que el usuario pueda mantener la sesión iniciada
 * entre ejecuciones de la aplicación, siempre que el refresh token siga
 * siendo válido en Supabase.</p>
 */
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    /**
     * Ruta donde se almacena el archivo JSON con el refresh token persistido
     * del usuario actual.
     *
     * <p>Formato esperado del contenido:</p>
     * <pre>
     * {
     *   "refresh_token": "xxxxx.yyyyy.zzzzz"
     * }
     * </pre>
     */
    private static final Path SESSION_FILE =
            Path.of(System.getProperty("user.home"), ".romraider", "session.json");

    /**
     * Guarda el refresh token en disco dentro de {@link #SESSION_FILE}.
     *
     * <p>Si la carpeta no existe, se crea automáticamente.
     * En caso de error, se registra en el log pero no se lanza excepción.</p>
     *
     * @param refreshToken el token a almacenar.
     */
    public static void saveSession(String refreshToken) {
        try {
            Files.createDirectories(SESSION_FILE.getParent());

            JSONObject json = new JSONObject();
            json.put("refresh_token", refreshToken);

            Files.writeString(SESSION_FILE, json.toString());
            logger.info("Refresh token guardado correctamente en {}", SESSION_FILE);

        } catch (IOException e) {
            logger.error("No se pudo guardar el refresh token en {}", SESSION_FILE, e);
        }
    }

    /**
     * Carga el refresh token desde el archivo de sesión si existe.
     *
     * <p>Si el archivo {@link #SESSION_FILE} no existe, está corrupto
     * o no puede leerse, se devuelve {@code null} y se informa mediante log.</p>
     *
     * @return el refresh token o {@code null} si no existe o no se pudo leer.
     */
    public static String loadSession() {
        try {
            if (Files.exists(SESSION_FILE)) {
                String content = Files.readString(SESSION_FILE).trim();
                JSONObject json = new JSONObject(content);

                String token = json.optString("refresh_token", null);

                logger.info("Refresh token cargado desde {}", SESSION_FILE);
                return token;
            } else {
                logger.info("No existe un archivo de sesión en {}", SESSION_FILE);
            }
        } catch (Exception e) {
            logger.error("No se pudo leer el archivo de sesión {}", SESSION_FILE, e);
        }

        return null;
    }

    /**
     * Elimina el archivo que contiene el refresh token persistido.
     *
     * <p>Si el archivo no existe, simplemente se registra una traza informativa.
     * En caso de error de E/S, se deja constancia en el log.</p>
     */
    public static void clearSession() {
        try {
            if (Files.deleteIfExists(SESSION_FILE)) {
                logger.info("Archivo de sesión eliminado: {}", SESSION_FILE);
            } else {
                logger.info("No había archivo de sesión para eliminar en {}", SESSION_FILE);
            }
        } catch (IOException e) {
            logger.error("No se pudo eliminar el archivo de sesión {}", SESSION_FILE, e);
        }
    }
}
