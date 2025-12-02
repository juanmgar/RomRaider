package com.romraider.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Utilidad para cargar, leer, modificar y guardar propiedades
 * desde un archivo de configuración.
 * <p>
 * Envuelve la clase {@link Properties} y añade una API sencilla
 * para trabajar con el fichero asociado, manteniendo internamente
 * la ruta del archivo para poder persistir cambios posteriormente.
 */
public class PropertyUtils {

    /**
     * Logger para registrar las operaciones de carga y guardado,
     * así como los cambios realizados sobre las propiedades.
     */
    private static final Logger logger = LoggerFactory.getLogger(PropertyUtils.class);

    /**
     * Conjunto de propiedades cargadas desde el archivo.
     */
    private final Properties properties = new Properties();

    /**
     * Ruta absoluta al archivo de propiedades asociado a esta instancia.
     */
    private final String filePath;

    /**
     * Carga un archivo de propiedades existente en el sistema.
     * <p>
     * El archivo indicado por {@code filePath} debe existir y ser legible;
     * en caso contrario se lanzará una {@link IOException}.
     *
     * @param filePath ruta absoluta al archivo de configuración.
     * @throws IOException si el archivo no existe o no puede leerse.
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
     * @param key clave de la propiedad.
     * @return valor asociado a la clave o {@code null} si no existe.
     */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /**
     * Recupera el valor de una propiedad o un valor por defecto si no existe.
     *
     * @param key          clave de la propiedad.
     * @param defaultValue valor a devolver si la clave no existe.
     * @return valor encontrado o {@code defaultValue} si la clave no está definida.
     */
    public String getOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Establece una propiedad en memoria (no se guarda en disco
     * hasta llamar al método {@link #save(String)}).
     *
     * @param key   clave a modificar o crear.
     * @param value nuevo valor asociado a la clave.
     */
    public void set(String key, String value) {
        logger.debug("Actualizando propiedad '{}' = '{}'", key, value);
        properties.setProperty(key, value);
    }

    /**
     * Guarda todas las propiedades en el archivo asociado.
     * <p>
     * Sobrescribe el contenido previo del archivo de propiedades.
     *
     * @param comments comentario que se insertará en el encabezado del archivo.
     * @throws IOException si ocurre un error durante la escritura.
     */
    public void save(String comments) throws IOException {
        logger.info("Guardando archivo de propiedades en disco...");
        try (OutputStream out = new FileOutputStream(filePath)) {
            properties.store(out, comments);
            logger.info("Archivo de propiedades guardado correctamente.");
        }
    }

    /**
     * Devuelve el objeto {@link Properties} subyacente.
     * <p>
     * Puede utilizarse para operaciones avanzadas que no estén cubiertas
     * por los métodos de conveniencia de esta clase.
     *
     * @return objeto {@link Properties} actualmente cargado.
     */
    public Properties getProperties() {
        return properties;
    }
}
