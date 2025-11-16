package com.romraider.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Cargador centralizado de secretos necesarios para la aplicación.
 *
 * Prioridad de carga:
 *  1. Variables de entorno (ideal para CI/CD y SonarCloud)
 *  2. Archivo local `/config/secrets.properties`
 *
 * Las claves soportadas son:
 *  - RAWG_API_KEY
 *  - SUPABASE_URL
 *  - SUPABASE_KEY
 *
 * Este diseño permite mantener configuraciones sensibles fuera del código fuente
 * y manejar entornos locales y remotos sin cambios adicionales.
 */
public class SecretsLoader {

    private static final Logger logger = LoggerFactory.getLogger(SecretsLoader.class);

    /** Propiedades cargadas desde secrets.properties, si existe. */
    private static final Properties props = new Properties();

    // Bloque estático: intenta cargar el archivo de secretos local
    static {
        try (InputStream in = SecretsLoader.class.getResourceAsStream("/config/secrets.properties")) {

            if (in != null) {
                props.load(in);
                logger.info("Archivo secrets.properties cargado correctamente.");
            } else {
                logger.info("No se encontró secrets.properties. Solo se usarán variables de entorno.");
            }

        } catch (Exception e) {
            logger.error("Error cargando secrets.properties: {}", e.getMessage());
        }
    }

    /**
     * Obtiene un valor secreto buscándolo primero en variables de entorno
     * y luego en el archivo secrets.properties.
     *
     * @param key nombre de la clave (ej. RAWG_API_KEY)
     * @return valor encontrado o cadena vacía si no existe
     */
    public static String get(String key) {

        // 1) Variables de entorno tienen prioridad
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            logger.debug("Valor obtenido desde variable de entorno: {}", key);
            return envValue;
        }

        // 2) Fallback: archivo properties local
        String fileValue = props.getProperty(key, "");
        if (!fileValue.isEmpty()) {
            logger.debug("Valor obtenido desde secrets.properties: {}", key);
        } else {
            logger.warn("Clave '{}' no encontrada ni en entorno ni en secrets.properties", key);
        }

        return fileValue;
    }

    /**
     * @return API Key para RAWG.io
     */
    public static String getRawgApiKey() {
        return get("RAWG_API_KEY");
    }

    /**
     * @return URL del backend Supabase
     */
    public static String getSupabaseUrl() {
        return get("SUPABASE_URL");
    }

    /**
     * @return clave de acceso (anon/key) para Supabase
     */
    public static String getSupabaseKey() {
        return get("SUPABASE_KEY");
    }
}
