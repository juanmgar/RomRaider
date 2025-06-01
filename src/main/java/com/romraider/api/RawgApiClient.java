package com.romraider.api;

import com.romraider.utils.AppInitializer;
import com.romraider.utils.PropertyUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class RawgApiClient {

    public static class RomInfo {
        public final String descripcion;
        public final String imageUrl;

        public RomInfo(String descripcion, String imageUrl) {
            this.descripcion = descripcion;
            this.imageUrl = imageUrl;
        }
    }

    public static RomInfo obtenerInfo(String titulo) {
        try {
            PropertyUtils secrets = AppInitializer.loadSecrets();
            String rawgAPIKey = secrets.get("RAWG_API_KEY");

            String searchUrl = "https://api.rawg.io/api/games?search=" +
                    URLEncoder.encode(titulo, "UTF-8") +
                    "&key=" + rawgAPIKey;

            HttpURLConnection conn = (HttpURLConnection) new URL(searchUrl).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line);
            reader.close();

            JSONObject jsonObj = new JSONObject(json.toString());
            JSONArray results = jsonObj.getJSONArray("results");

            if (results.length() > 0) {
                int gameId = results.getJSONObject(0).getInt("id");

                String detailUrl = "https://api.rawg.io/api/games/" + gameId + "?key=" + rawgAPIKey;
                HttpURLConnection detailConn = (HttpURLConnection) new URL(detailUrl).openConnection();
                detailConn.setRequestMethod("GET");

                BufferedReader detailReader = new BufferedReader(new InputStreamReader(detailConn.getInputStream()));
                StringBuilder detailJson = new StringBuilder();
                while ((line = detailReader.readLine()) != null) detailJson.append(line);
                detailReader.close();

                JSONObject detailObj = new JSONObject(detailJson.toString());

                String description = detailObj.getString("description_raw")
                        .replaceAll("\\s+", " ")
                        .replaceAll("<[^>]*>", "")
                        .trim();

                String imageUrl = detailObj.optString("background_image", null);

                return new RomInfo(description, imageUrl);
            }
        } catch (Exception e) {
            System.err.println("Error en API RAWG: " + e.getMessage());
        }

        return null;
    }
}
