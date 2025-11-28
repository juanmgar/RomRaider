package com.romraider.utils;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18nUtils {

    private static ResourceBundle bundle;

    /**
     * Carga el idioma (ej: "en", "es")
     */
    public static void load(String lang) {
        Locale locale = new Locale(lang);
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    /**
     * Acceso principal: I18n.get("clave")
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }
}
