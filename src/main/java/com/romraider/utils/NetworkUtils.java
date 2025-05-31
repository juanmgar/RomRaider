package com.romraider.utils;

import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkUtils {

    public static boolean isInternetAvailable() {
        try {
            URL url = new URL("https://supabase.co");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.connect();
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
