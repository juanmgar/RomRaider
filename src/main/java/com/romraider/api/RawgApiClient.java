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
 *
 * <p>Se utiliza para:</p>
 * <ul>
 *     <li>Buscar un videojuego por título</li>
 *     <li>Obtener descripción del juego</li>
 *     <li>Obtener la imagen principal (background_image)</li>
 * </ul>
 *
 * <p>
 * La clase expone un método principal: {@link #obtenerInfo(String)}, que devuelve
 * un objeto {@link RomInfo} con los datos obtenidos o {@code null} en caso de error
 * o ausencia de resultados.
 * </p>
 */
public class RawgApiClient {

    private static final Logger logger = LoggerFactory.getLogger(RawgApiClient.class);

    /**
     * DTO simple que representa la información relevante obtenida de RAWG.
     *
     * <p>Incluye:</p>
     * <ul>
     *     <li>Descripción en texto plano (sin HTML)</li>
     *     <li>URL de la imagen principal</li>
     * </ul>
     */
    public static class RomInfo {
        /** Descripción textual limpia del juego. */
        public final String descripcion;

        /** URL hacia la imagen principal del juego en RAWG. */
        public final String imageUrl;

        /**
         * Crea un contenedor de información obtenida desde RAWG.io.
         *
         * @param descripcion descripción del juego en texto plano
         * @param imageUrl URL de la imagen principal o null si no existe
         */
        public RomInfo(String descripcion, String imageUrl) {
            this.descripcion = descripcion;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Realiza una búsqueda en RAWG.io usando el título indicado.
     *
     * <p>El proceso es:</p>
     * <ol>
     *     <li>Buscar juegos mediante {@code /games?search=<titulo>}</li>
     *     <li>Obtener el primer resultado de búsqueda</li>
     *     <li>Consultar sus detalles mediante {@code /games/{id}}</li>
     *     <li>Extraer:
     *         <ul>
     *             <li>{@code description_raw}</li>
     *             <li>{@code background_image}</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * <p>En caso de error (sin resultados, excepción, API key inválida, etc.),
     * devuelve {@code null}.</p>
     *
     * @param titulo título o nombre aproximado de la ROM
     * @return instancia {@link RomInfo} con descripción e imagen, o {@code null} si no se encuentra información
     */
    public static RomInfo obtenerInfo(String titulo) {
        try {

            // API key obtenida desde configuración
            String rawgKey = SecretsLoader.getRawgApiKey();
            if (rawgKey == null || rawgKey.isBlank()) {
                logger.error("La API key de RAWG no está configurada.");
                return null;
            }

            logger.info("Consultando RAWG API para el título: {}", titulo);

            // --------------------------------------------------------------------
            // 1) Primera llamada: buscar juegos por título
            // --------------------------------------------------------------------
            String searchUrl =
                    "https://api.rawg.io/api/games?search=" +
                            URLEncoder.encode(titulo, "UTF-8") +
                            "&key=" + rawgKey;

            HttpURLConnection conn =
                    (HttpURLConnection) new URL(searchUrl).openConnection();
            conn.setRequestMethod(APIsConstants.GET);

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

            // --------------------------------------------------------------------
            // 2) Segunda llamada: obtener detalles del juego
            // --------------------------------------------------------------------
            String detailUrl = "https://api.rawg.io/api/games/" + gameId + "?key=" + rawgKey;

            HttpURLConnection detailConn =
                    (HttpURLConnection) new URL(detailUrl).openConnection();
            detailConn.setRequestMethod(APIsConstants.GET);

            StringBuilder detailJson = new StringBuilder();
            try (BufferedReader detailReader =
                         new BufferedReader(new InputStreamReader(detailConn.getInputStream()))) {

                String line;
                while ((line = detailReader.readLine()) != null) detailJson.append(line);
            }

            JSONObject detailObj = new JSONObject(detailJson.toString());

            // --------------------------------------------------------------------
            // 3) Procesar descripción
            // --------------------------------------------------------------------
            /*
             * RAWG devuelve "description_raw" como texto plano. En algunos casos,
             * ciertas APIs pueden devolver HTML, por lo que se aplica una limpieza
             * básica eliminando etiquetas.
             */
            String description = detailObj.optString("description_raw", "")
                    .replaceAll("<[^>]*>", "")  // eliminar HTML
                    .replaceAll("\\s+", " ")     // normalizar espacios
                    .trim();

            if (description.isEmpty()) {
                logger.debug("RAWG no proporcionó descripción para '{}'", titulo);
            }

            // --------------------------------------------------------------------
            // 4) Imagen principal
            // --------------------------------------------------------------------
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
