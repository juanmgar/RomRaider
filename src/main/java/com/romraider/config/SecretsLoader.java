package com.romraider.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Cargador centralizado de secretos necesarios para la aplicación.
 *
 * <p>Los valores se leen exclusivamente del archivo incluido dentro del JAR:</p>
 * <pre>
 *     /config/secrets.properties
 * </pre>
 *
 * <p>Este archivo se inyecta en el artefacto final desde GitHub Actions,
 * evitando que las claves estén en el repositorio.</p>
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
     * @param key nombre de la clave tal y como aparece en {@code secrets.properties}
     * @return valor asociado a la clave o cadena vacía si no existe
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

    /**
     * Devuelve la API key utilizada para consumir la API de RAWG.io.
     *
     * <p>Se corresponde con la clave {@code RAWG_API_KEY} del fichero
     * {@code secrets.properties}.</p>
     *
     * @return API key de RAWG, o cadena vacía si no está definida
     */
    public static String getRawgApiKey() {
        return get("RAWG_API_KEY");
    }

    /**
     * Devuelve la URL base del proyecto Supabase.
     *
     * <p>Se corresponde con la clave {@code SUPABASE_URL} del fichero
     * {@code secrets.properties}.</p>
     *
     * @return URL base de Supabase (por ejemplo, {@code https://xxxxx.supabase.co}),
     *         o cadena vacía si no está definida
     */
    public static String getSupabaseUrl() {
        return get("SUPABASE_URL");
    }

    /**
     * Devuelve la API key utilizada para autenticarse contra Supabase.
     *
     * <p>Se corresponde con la clave {@code SUPABASE_KEY} del fichero
     * {@code secrets.properties}.</p>
     *
     * @return API key de Supabase, o cadena vacía si no está definida
     */
    public static String getSupabaseKey() {
        return get("SUPABASE_KEY");
    }
}
