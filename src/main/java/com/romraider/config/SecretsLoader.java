package com.romraider.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Cargador centralizado de secretos necesarios para la aplicación.
 *
 * Los valores se leen exclusivamente del archivo incluido dentro del JAR:
 *      /config/secrets.properties
 *
 * Este archivo se inyecta en el artefacto final desde GitHub Actions,
 * evitando que las claves estén en el repositorio.
 */
public class SecretsLoader {

    private static final Logger logger = LoggerFactory.getLogger(SecretsLoader.class);
    private static final Properties props = new Properties();

    static {
        try (InputStream in = SecretsLoader.class.getResourceAsStream("/config/secrets.properties")) {

            if (in != null) {
                props.load(in);
                logger.info("Archivo secrets.properties cargado correctamente desde el JAR.");
            } else {
                logger.error("ERROR: No se encontró el archivo /config/secrets.properties dentro del JAR.");
                logger.error("La aplicación no podrá conectarse a Supabase ni RAWG.");
            }

        } catch (Exception e) {
            logger.error("Error cargando secrets.properties: {}", e.getMessage());
        }
    }

    /**
     * Obtiene un valor secreto desde el archivo embebido.
     *
     * @param key Nombre de la clave
     * @return Valor o cadena vacía
     */
    public static String get(String key) {
        String value = props.getProperty(key, "");

        if (value.isEmpty()) {
            logger.error("Clave '{}' no encontrada en secrets.properties", key);
        } else {
            logger.debug("Clave '{}' cargada correctamente.", key);
        }

        return value;
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
