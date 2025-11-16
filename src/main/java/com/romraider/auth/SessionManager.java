package com.romraider.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gestiona la sesión persistida del usuario.
 * <p>
 * Se encarga de guardar, cargar y eliminar un token de sesión
 * en el directorio del usuario (~/.romraider/session.json).
 * <p>
 * Esta clase no interpreta el contenido del token; simplemente lo almacena.
 */
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    /**
     * Ruta donde se almacena el token de sesión persistido
     */
    private static final Path SESSION_FILE =
            Path.of(System.getProperty("user.home"), ".romraider", "session.json");

    /**
     * Guarda el token de sesión en disco.
     *
     * @param token el token a almacenar (no se valida su formato).
     */
    public static void saveSession(String token) {
        try {
            // Nos aseguramos de que el directorio exista antes de escribir.
            Files.createDirectories(SESSION_FILE.getParent());

            Files.writeString(SESSION_FILE, token);
            logger.info("Token guardado correctamente en {}", SESSION_FILE);

        } catch (IOException e) {
            logger.error("No se pudo guardar el token en {}", SESSION_FILE, e);
        }
    }

    /**
     * Carga el token de sesión si existe en disco.
     *
     * @return el token almacenado o null si no existe o no se pudo leer.
     */
    public static String loadSession() {
        try {
            if (Files.exists(SESSION_FILE)) {
                String token = Files.readString(SESSION_FILE).trim();
                logger.info("Token cargado desde {}", SESSION_FILE);
                return token;
            } else {
                logger.info("No existe un archivo de sesión en {}", SESSION_FILE);
            }
        } catch (IOException e) {
            logger.error("No se pudo leer el archivo de sesión {}", SESSION_FILE, e);
        }

        return null;
    }

    /**
     * Elimina el archivo que contiene el token persistido.
     * <p>
     * Útil al cerrar sesión o invalidar credenciales.
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
