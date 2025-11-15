package com.romraider.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utilidad para comprobar si hay conexión a Internet.
 *
 * Se realiza una conexión HTTP ligera contra Supabase para verificar
 * si existe conectividad real y no únicamente conexión local.
 */
public class NetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * Comprueba si Internet está disponible realizando una conexión
     * HTTP sencilla a "https://supabase.co".
     *
     * @return true si hay conexión, false si no
     */
    public static boolean isInternetAvailable() {
        try {
            URL url = new URL("https://supabase.co");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000); // Evita bloqueos largos
            conn.connect();

            int response = conn.getResponseCode();
            boolean ok = response == 200;

            logger.debug("Resultado de la comprobación de conectividad: {}", ok);

            return ok;

        } catch (Exception e) {
            logger.warn("No hay conexión a Internet: {}", e.getMessage());
            return false;
        }
    }
}
