package com.romraider.api;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.AppInitializer;
import com.romraider.utils.PropertyUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SupabaseSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseSyncService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
    private static final String TIMESTAMP_FILE = System.getProperty("user.home") + "/.romraider/last_sync.txt";

    private static final PlataformaService plataformaService = new PlataformaService();
    private static final RomService romService = new RomService();

    public static void syncWithSupabase() {
        String userId = SupabaseAuthService.getUserId();
        if (userId == null) {
            logger.warn("No user logged in, skipping sync.");
            return;
        }

        try {
            LocalDateTime localTimestamp = getLocalTimestamp();
            LocalDateTime remoteTimestamp = getRemoteTimestamp(userId);

            if (remoteTimestamp.isAfter(localTimestamp)) {
                logger.info("Remote data is newer. Syncing from Supabase.");
                downloadAllData(userId);
                saveLocalTimestamp(remoteTimestamp);
            } else if (localTimestamp.isAfter(remoteTimestamp)) {
                logger.info("Local data is newer. Syncing to Supabase.");
                uploadAllData(userId);
            } else {
                logger.info("Data already up to date. No sync needed.");
            }

        } catch (Exception e) {
            logger.error("Error during synchronization", e);
        }
    }

    private static LocalDateTime getLocalTimestamp() {
        File file = new File(TIMESTAMP_FILE);
        if (!file.exists()) return LocalDateTime.MIN;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return LocalDateTime.parse(reader.readLine(), formatter);
        } catch (IOException e) {
            logger.warn("Could not read local timestamp", e);
            return LocalDateTime.MIN;
        }
    }

    private static void saveLocalTimestamp(LocalDateTime timestamp) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(TIMESTAMP_FILE))) {
            writer.write(timestamp.format(formatter));
        } catch (IOException e) {
            logger.error("Could not save local timestamp", e);
        }
    }

    private static LocalDateTime getRemoteTimestamp(String userId) throws IOException {
        PropertyUtils secrets = AppInitializer.loadSecrets();

        String baseUrl = secrets.get("SUPABASE_URL");
        String apiKey = secrets.get("SUPABASE_KEY");

        logger.info("DEBUG - API KEY loaded: {}", apiKey);

        String url = baseUrl + "/rest/v1/sync_status?user_id=eq." + userId + "&select=last_updated";
        logger.info("GET {}", url); // Log de la URL
        logger.info("Header apikey: {}", apiKey); // Cuidado: solo mostrar en desarrollo

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", apiKey);

        try (InputStream is = conn.getInputStream(); Scanner scanner = new Scanner(is)) {
            String response = scanner.useDelimiter("\\A").next();
            JSONArray jsonArray = new JSONArray(response);
            if (jsonArray.length() > 0) {
                String timestampStr = jsonArray.getJSONObject(0).getString("last_updated");
                return LocalDateTime.parse(timestampStr, formatter);
            }
        }

        return LocalDateTime.MIN;
    }

    private static void updateRemoteTimestamp(String userId, LocalDateTime timestamp) throws IOException {
        PropertyUtils secrets = AppInitializer.loadSecrets();
        String baseUrl = secrets.get("SUPABASE_URL");
        String apiKey = secrets.get("SUPABASE_KEY");

        String url = baseUrl + "/rest/v1/sync_status";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "resolution=merge-duplicates");
        conn.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("user_id", userId);
        body.put("last_updated", timestamp.format(formatter));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes());
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            logger.info("sync_status actualizado correctamente.");
        } else {
            logger.warn("Error actualizando sync_status. Código: {}", code);
        }
    }

    private static void uploadAllData(String userId) {
        try {
            PropertyUtils secrets = AppInitializer.loadSecrets();
            String baseUrl = secrets.get("SUPABASE_URL");
            String apiKey = secrets.get("SUPABASE_KEY");

            // 1. Obtener datos locales
            List<Plataforma> plataformas = plataformaService.obtenerTodasConRoms();

            // 2. Borrar datos anteriores en Supabase
            borrarDatosRemotos(userId, baseUrl, apiKey);

            // 3. Subir plataformas y recoger sus nuevos UUIDs remotos
            Map<Integer, String> plataformaIdMap = new HashMap<>();
            for (Plataforma plataforma : plataformas) {
                JSONObject json = new JSONObject();
                json.put("nombre", plataforma.getNombre());
                json.put("extension_rom", plataforma.getExtensionRom());
                json.put("carpeta", plataforma.getCarpeta());
                json.put("user_id", userId);

                logger.info("Enviando plataforma a Supabase:\n{}", json.toString(2));

                HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/rest/v1/plataformas").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", apiKey);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("[" + json.toString() + "]").getBytes());
                }

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("[" + json.toString() + "]").getBytes());
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 201) {
                    Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String response = scanner.hasNext() ? scanner.next() : "";
                    JSONArray array = new JSONArray(response);
                    String remoteId = array.getJSONObject(0).getString("id");
                    plataformaIdMap.put(plataforma.getId(), remoteId);
                } else {
                    logger.warn("Error subiendo plataforma '{}'. Status {}", plataforma.getNombre(), responseCode);
                }
            }

            // 4. Subir ROMs
            for (Plataforma plataforma : plataformas) {
                String remotePlataformaId = plataformaIdMap.get(plataforma.getId());
                if (remotePlataformaId == null) continue;

                for (Rom rom : plataforma.getRoms()) {
                    JSONObject json = new JSONObject();
                    json.put("titulo", rom.getTitulo());
                    json.put("descripcion", rom.getDescripcion());
                    json.put("imagen", rom.getImagen());
                    json.put("favorito", rom.isFavorito());
                    json.put("jugado", rom.isJugado());
                    json.put("user_id", userId);
                    json.put("plataforma_id", remotePlataformaId);

                    HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/rest/v1/roms").openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("apikey", apiKey);
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(("[" + json.toString() + "]").getBytes());
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 201) {
                        logger.warn("Error subiendo ROM '{}'. Status {}", rom.getTitulo(), responseCode);
                    }
                }
            }

            // 5. Actualizar tabla sync_status
            actualizarRemoteTimestamp(userId, LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Error al subir datos a Supabase", e);
        }
    }


    private static void downloadAllData(String userId) {
        try {
            PropertyUtils secrets = AppInitializer.loadSecrets();
            String baseUrl = secrets.get("SUPABASE_URL");
            String apiKey = secrets.get("SUPABASE_KEY");

            // 1. Descargar plataformas remotas
            URL plataformasUrl = new URL(baseUrl + "/rest/v1/plataformas?user_id=eq." + userId + "&select=*");
            HttpURLConnection connPlataformas = (HttpURLConnection) plataformasUrl.openConnection();
            connPlataformas.setRequestMethod("GET");
            connPlataformas.setRequestProperty("apikey", apiKey);
            connPlataformas.setRequestProperty("Authorization", "Bearer " + apiKey);

            JSONArray plataformasJson;
            try (Scanner scanner = new Scanner(connPlataformas.getInputStream()).useDelimiter("\\A")) {
                String response = scanner.hasNext() ? scanner.next() : "[]";
                plataformasJson = new JSONArray(response);
            }

            // Mapa para asociar los UUIDs remotos con los IDs locales
            Map<String, Integer> remoteIdToLocalIdMap = new HashMap<>();

            // 2. Limpiar tablas locales
            romService.eliminarTodas(); // ← necesitas añadir este método si no lo tienes
            plataformaService.eliminarTodas();

            // 3. Guardar plataformas localmente
            for (int i = 0; i < plataformasJson.length(); i++) {
                JSONObject obj = plataformasJson.getJSONObject(i);
                Plataforma plataforma = new Plataforma();
                plataforma.setNombre(obj.getString("nombre"));
                plataforma.setExtensionRom(obj.getString("extension_rom"));
                plataforma.setCarpeta(obj.getString("carpeta"));

                plataformaService.guardar(plataforma);
                remoteIdToLocalIdMap.put(obj.getString("id"), plataforma.getId());
            }

            // 4. Descargar ROMs remotas
            URL romsUrl = new URL(baseUrl + "/rest/v1/roms?user_id=eq." + userId + "&select=*");
            HttpURLConnection connRoms = (HttpURLConnection) romsUrl.openConnection();
            connRoms.setRequestMethod("GET");
            connRoms.setRequestProperty("apikey", apiKey);
            connRoms.setRequestProperty("Authorization", "Bearer " + apiKey);

            JSONArray romsJson;
            try (Scanner scanner = new Scanner(connRoms.getInputStream()).useDelimiter("\\A")) {
                String response = scanner.hasNext() ? scanner.next() : "[]";
                romsJson = new JSONArray(response);
            }

            // 5. Guardar ROMs localmente
            for (int i = 0; i < romsJson.length(); i++) {
                JSONObject obj = romsJson.getJSONObject(i);
                String remotePlataformaId = obj.getString("plataforma_id");

                if (!remoteIdToLocalIdMap.containsKey(remotePlataformaId)) continue;

                Rom rom = new Rom();
                rom.setTitulo(obj.getString("titulo"));
                rom.setDescripcion(obj.getString("descripcion"));
                rom.setImagen(obj.getString("imagen"));
                rom.setFavorito(obj.getBoolean("favorito"));
                rom.setJugado(obj.getBoolean("jugado"));

                int localPlataformaId = remoteIdToLocalIdMap.get(remotePlataformaId);
                Plataforma plataforma = plataformaService.buscarPorId(localPlataformaId);
                rom.setPlataforma(plataforma);

                romService.guardar(rom);
            }

            // 6. Guardar timestamp local
            saveLocalTimestamp(LocalDateTime.now());
            logger.info("Datos descargados correctamente desde Supabase");

        } catch (Exception e) {
            logger.error("Error al descargar datos desde Supabase", e);
        }
    }

    private static void borrarDatosRemotos(String userId, String baseUrl, String apiKey) throws IOException {
        // Borrar ROMs primero (por FK con plataformas)
        URL romsUrl = new URL(baseUrl + "/rest/v1/roms?user_id=eq." + userId);
        HttpURLConnection romsConn = (HttpURLConnection) romsUrl.openConnection();
        romsConn.setRequestMethod("DELETE");
        romsConn.setRequestProperty("apikey", apiKey);
        romsConn.setRequestProperty("Authorization", "Bearer " + apiKey);
        romsConn.setRequestProperty("Prefer", "resolution=merge-duplicates");
        int romsResponse = romsConn.getResponseCode();
        if (romsResponse >= 200 && romsResponse < 300) {
            logger.info("ROMs eliminadas del usuario {}", userId);
        } else {
            logger.warn("No se pudieron eliminar ROMs del usuario. Código: {}", romsResponse);
        }

        // Borrar plataformas
        URL plataformasUrl = new URL(baseUrl + "/rest/v1/plataformas?user_id=eq." + userId);
        HttpURLConnection plataformasConn = (HttpURLConnection) plataformasUrl.openConnection();
        plataformasConn.setRequestMethod("DELETE");
        plataformasConn.setRequestProperty("apikey", apiKey);
        plataformasConn.setRequestProperty("Authorization", "Bearer " + apiKey);
        plataformasConn.setRequestProperty("Prefer", "resolution=merge-duplicates");
        int plataformasResponse = plataformasConn.getResponseCode();
        if (plataformasResponse >= 200 && plataformasResponse < 300) {
            logger.info("Plataformas eliminadas del usuario {}", userId);
        } else {
            logger.warn("No se pudieron eliminar plataformas. Código: {}", plataformasResponse);
        }
    }

    private static void actualizarRemoteTimestamp(String userId, LocalDateTime timestamp) throws IOException {
        PropertyUtils secrets = AppInitializer.loadSecrets();
        String baseUrl = secrets.get("SUPABASE_URL");
        String apiKey = secrets.get("SUPABASE_KEY");

        String url = baseUrl + "/rest/v1/sync_status";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("apikey", apiKey);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Prefer", "resolution=merge-duplicates");
        conn.setDoOutput(true);

        JSONObject body = new JSONObject();
        body.put("user_id", userId);
        body.put("last_updated", timestamp.format(formatter));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            logger.info("sync_status actualizado correctamente.");
        } else {
            logger.warn("No se pudo actualizar sync_status. Código: {}", responseCode);
        }
    }


}