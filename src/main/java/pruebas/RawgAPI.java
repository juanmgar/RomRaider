package pruebas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RawgAPI {

    private static final String API_KEY = "e6a0126080a14743825d61ecc3e5a349";

    public static String buscarDescripcionJuego(String titulo, String extension) {
        try {
            int rawgPlatformId = mapearPlataformaRAWG(extension);

            String rawgUrl = "https://api.rawg.io/api/games?search=" +
                    java.net.URLEncoder.encode(titulo, "UTF-8") +
                    "&platforms=" + rawgPlatformId +
                    "&key=" + API_KEY;
            System.out.println("URL: " + rawgUrl);
            URL url = new URL(rawgUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Leer respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder jsonStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStr.append(line);
            }
            reader.close();

            // Parsear JSON
            JSONObject json = new JSONObject(jsonStr.toString());
            JSONArray results = json.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject game = results.getJSONObject(0);
                String name = game.getString("name");
                int id = game.getInt("id");

                // Hacer segunda petición para obtener la descripción
                String detailUrl = "https://api.rawg.io/api/games/" + id + "?key=" + API_KEY;
                URL detail = new URL(detailUrl);
                HttpURLConnection detailConn = (HttpURLConnection) detail.openConnection();
                detailConn.setRequestMethod("GET");

                BufferedReader detailReader = new BufferedReader(new InputStreamReader(detailConn.getInputStream()));
                StringBuilder detailStr = new StringBuilder();
                while ((line = detailReader.readLine()) != null) {
                    detailStr.append(line);
                }
                detailReader.close();

                JSONObject detailJson = new JSONObject(detailStr.toString());

                return detailJson.getString("description_raw")
                        .replaceAll("\\s+", " ")  // Reemplaza saltos de línea, tabulaciones y espacios múltiples por uno solo
                        .replaceAll("<[^>]*>", "") // Por si quedara algo de HTML residual (por seguridad extra)
                        .trim();
            }

        } catch (Exception e) {
            System.err.println("Error buscando descripción: " + e.getMessage());
        }

        return null;
    }

    private static int mapearPlataformaRAWG(String extension) {
        return switch (extension.toLowerCase()) {
            case "nes" -> 49;
            case "sfc", "smc" -> 79;       // SNES
            case "gba" -> 24;
            case "gb" -> 26;
            case "gbc" -> 43;
            case "n64", "z64" -> 83;
            case "gen", "md" -> 167;       // Sega Mega Drive
            case "sms" -> 74;
            default -> 0; // 0 = sin filtrar (más genérico)
        };
    }

}
