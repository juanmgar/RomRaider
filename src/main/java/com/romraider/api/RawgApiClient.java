package com.romraider.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class RawgApiClient {

    private static final String API_KEY = "e6a0126080a14743825d61ecc3e5a349";

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
            String searchUrl = "https://api.rawg.io/api/games?search=" +
                    URLEncoder.encode(titulo, "UTF-8") +
                    "&key=" + API_KEY;

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

                String detailUrl = "https://api.rawg.io/api/games/" + gameId + "?key=" + API_KEY;
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

    public static String descargarImagen(String imageUrl, String romTitle, int romId) {
        try {
            String safeTitle = romTitle.replaceAll("[^a-zA-Z0-9]", "_");
            String extension = imageUrl.substring(imageUrl.lastIndexOf('.') + 1);
            String filename = safeTitle + "_" + romId + "." + extension;
            File destDir = new File(System.getProperty("user.home"), ".romraider/images");
            destDir.mkdirs();
            File destFile = new File(destDir, filename);

            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return destFile.getAbsolutePath();
            }
        } catch (IOException e) {
            System.err.println("Error descargando imagen: " + e.getMessage());
            return null;
        }
    }

}
