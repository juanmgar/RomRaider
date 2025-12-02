package com.romraider.utils;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utilidad para la gestión de internacionalización (i18n) mediante {@link ResourceBundle}.
 * <p>
 * Permite cargar un idioma concreto y obtener cadenas traducidas a partir de claves.
 */
public class I18nUtils {

    /**
     * Recurso de propiedades actualmente cargado para el idioma seleccionado.
     */
    private static ResourceBundle bundle;

    /**
     * Carga el archivo de propiedades correspondiente al idioma indicado.
     * <p>
     * Ejemplo de uso:
     * <pre>{@code
     * I18nUtils.load("es"); // Carga i18n/messages_es.properties
     * }</pre>
     *
     * @param lang código de idioma ISO (por ejemplo: {@code "en"}, {@code "es"}).
     *             Debe existir un fichero {@code messages_lang.properties} dentro de {@code /i18n}.
     */
    public static void load(String lang) {
        Locale locale = new Locale(lang);
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    /**
     * Obtiene una cadena internacionalizada a partir de una clave.
     * <p>
     * Si la clave no existe, devuelve un marcador en formato {@code ??clave??}.
     *
     * @param key clave definida en el archivo de propiedades.
     * @return el texto traducido correspondiente o {@code ??key??} si no existe.
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    /**
     * Devuelve el {@link ResourceBundle} actualmente cargado.
     *
     * @return el bundle activo; puede ser {@code null} si no se ha llamado antes a {@link #load(String)}.
     */
    public static ResourceBundle getBundle() {
        return bundle;
    }
}
