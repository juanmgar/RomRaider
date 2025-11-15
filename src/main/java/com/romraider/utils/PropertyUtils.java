package com.romraider.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Utilidad para cargar, leer, modificar y guardar propiedades
 * desde un archivo de configuración.
 *
 * Envuelve la clase {@link Properties} y añade una API sencilla
 * para trabajar con el fichero asociado.
 */
public class PropertyUtils {

    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);

    private final Properties properties = new Properties();
    private final String filePath;

    /**
     * Carga un archivo de propiedades existente en el sistema.
     *
     * @param filePath ruta absoluta al archivo de configuración
     * @throws IOException si el archivo no existe o no puede leerse
     */
    public PropertyUtils(String filePath) throws IOException {
        this.filePath = filePath;

        logger.info("Cargando archivo de propiedades: {}", filePath);

        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
            logger.debug("Propiedades cargadas correctamente ({} claves)", properties.size());
        }
    }

    /**
     * Recupera el valor de una clave.
     *
     * @param key clave de la propiedad
     * @return valor o null si no existe
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Recupera el valor de una propiedad o un valor por defecto si no existe.
     *
     * @param key clave de la propiedad
     * @param defaultValue valor a devolver si la clave no existe
     * @return valor encontrado o defaultValue
     */
    public String getOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Establece una propiedad en memoria (no se guarda en disco
     * hasta llamar al método {@link #save(String)}).
     *
     * @param key clave a modificar
     * @param value nuevo valor
     */
    public void set(String key, String value) {
        logger.debug("Actualizando propiedad '{}' = '{}'", key, value);
        properties.setProperty(key, value);
    }

    /**
     * Guarda todas las propiedades en el archivo asociado.
     *
     * @param comments comentario que se insertará en el encabezado del archivo
     * @throws IOException si ocurre un error durante la escritura
     */
    public void save(String comments) throws IOException {
        logger.info("Guardando archivo de propiedades en disco...");
        try (OutputStream out = new FileOutputStream(filePath)) {
            properties.store(out, comments);
            logger.info("Archivo de propiedades guardado correctamente.");
        }
    }

    /**
     * Devuelve el objeto Properties subyacente.
     *
     * @return objeto {@link Properties}
     */
    public Properties getProperties() {
        return properties;
    }
}
