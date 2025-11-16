package com.romraider.api;

import com.romraider.config.SecretsLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Cliente para la API pública de RAWG.io.
 * <p>
 * Se utiliza para:
 * - Buscar un videojuego por título
 * - Obtener descripción e imagen de fondo (background_image)
 * <p>
 * La clase devuelve objetos {@link RomInfo}, que encapsulan los datos obtenidos.
 */
public class RawgApiClient {

    private static final Logger logger = LoggerFactory.getLogger(RawgApiClient.class);

    /**
     * DTO simple que representa la información relevante obtenida de RAWG.
     */
    public static class RomInfo {
        public final String descripcion;
        public final String imageUrl;

        public RomInfo(String descripcion, String imageUrl) {
            this.descripcion = descripcion;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Realiza una búsqueda en RAWG.io usando el título indicado.
     * <p>
     * Pasos que realiza:
     * 1. Llamada a /games?search=<titulo>
     * 2. Obtiene el primer resultado y su id
     * 3. Llama a /games/{id} para obtener detalles completos
     * 4. Extrae:
     * - description_raw
     * - background_image
     * <p>
     * En caso de error, devuelve null.
     *
     * @param titulo título o nombre aproximado de la ROM
     * @return RomInfo si se encuentra información, otherwise null
     */
    public static RomInfo obtenerInfo(String titulo) {
        try {

            String rawgKey = SecretsLoader.getRawgApiKey();
            if (rawgKey == null || rawgKey.isBlank()) {
                logger.error("La API key de RAWG no está configurada.");
                return null;
            }

            logger.info("Consultando RAWG API para el título: {}", titulo);

            // 1. Primera llamada: búsqueda por título
            String searchUrl =
                    "https://api.rawg.io/api/games?search=" +
                            URLEncoder.encode(titulo, "UTF-8") +
                            "&key=" + rawgKey;

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(searchUrl).openConnection();
            conn.setRequestMethod("GET");

            // Leer la respuesta JSON de la API de búsqueda
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) json.append(line);
            }

            JSONObject searchObj = new JSONObject(json.toString());
            JSONArray results = searchObj.getJSONArray("results");

            if (results.length() == 0) {
                logger.warn("No se encontraron resultados en RAWG para '{}'", titulo);
                return null;
            }

            int gameId = results.getJSONObject(0).getInt("id");
            logger.debug("Juego encontrado en RAWG (ID={}): {}", gameId, titulo);

            // 2. Segunda llamada: detalles del juego ---
            String detailUrl = "https://api.rawg.io/api/games/" + gameId + "?key=" + rawgKey;
            HttpURLConnection detailConn =
                    (HttpURLConnection) new URL(detailUrl).openConnection();
            detailConn.setRequestMethod("GET");

            StringBuilder detailJson = new StringBuilder();
            try (BufferedReader detailReader =
                         new BufferedReader(new InputStreamReader(detailConn.getInputStream()))) {

                String line;
                while ((line = detailReader.readLine()) != null) detailJson.append(line);
            }

            JSONObject detailObj = new JSONObject(detailJson.toString());

            // 3. Procesado de descripción
            /*
             * RAWG devuelve "description_raw" como texto plano.
             * En caso de que la API devuelva HTML, se limpia eliminando etiquetas.
             */
            String description = detailObj.optString("description_raw", "")
                    .replaceAll("<[^>]*>", "")  // eliminar etiquetas HTML
                    .replaceAll("\\s+", " ")     // normalizar espacios
                    .trim();

            if (description.isEmpty()) {
                logger.debug("RAWG no proporcionó descripción para '{}'", titulo);
            }

            // 4. Imagen principal
            String imageUrl = detailObj.optString("background_image", null);

            if (imageUrl == null) {
                logger.debug("RAWG no proporcionó imagen para '{}'", titulo);
            }

            logger.info("Información obtenida correctamente desde RAWG para '{}'", titulo);

            return new RomInfo(description, imageUrl);

        } catch (Exception e) {
            logger.error("Error consultando la API de RAWG para '{}': {}", titulo, e.getMessage());
            return null;
        }
    }
}
