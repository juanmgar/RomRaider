package com.romraider.api;

import com.romraider.config.SecretsLoader;
import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.SyncStateUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Servicio responsable de sincronizar las plataformas y ROMs
 * entre la base de datos local y el backend Supabase.
 *
 * Se basa en timestamps para decidir si descargar o subir datos,
 * y en la asociación user_id → data para almacenar colecciones separadas por usuario.
 */
public class SupabaseSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseSyncService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

    private static final PlataformaService plataformaService = new PlataformaService();
    private static final RomService romService = new RomService();

    /**
     * Sincroniza la base de datos local con Supabase.
     * La lógica decide si descargar o subir datos comparando timestamps locales y remotos.
     */
    public static void syncWithSupabase() {
        String userId = SupabaseAuthService.getUserId();
        if (userId == null) {
            logger.warn("No hay usuario autenticado. Sincronización omitida.");
            return;
        }

        try {
            boolean firstSync = SyncStateUtils.getLastUser() == null
                    || !userId.equals(SyncStateUtils.getLastUser());

            boolean hasLocalData = !plataformaService.obtenerTodas().isEmpty();

            if (firstSync) {
                logger.info("Primera sincronización detectada para el usuario {}. Descargando datos iniciales.", userId);
                downloadAllData(userId);
                return;
            }

            LocalDateTime lastSync = SyncStateUtils.getLastSync();
            LocalDateTime lastLocalEdit = SyncStateUtils.getLastLocalEdit();

            LocalDateTime remoteTimestamp = getRemoteTimestamp(userId);
            SyncStateUtils.updateLastRemoteSync(remoteTimestamp);

            logger.info(
                    "Estado de timestamps → última sync local: {}, última edición local: {}, remoto: {}",
                    lastSync, lastLocalEdit, remoteTimestamp
            );

            // Datos más nuevos en remoto
            if (remoteTimestamp.isAfter(lastSync) && remoteTimestamp.isAfter(lastLocalEdit)) {
                logger.info("Detectados datos más recientes en Supabase. Descargando...");
                downloadAllData(userId);
                return;
            }

            // Datos más nuevos en local o base local vacía
            if ((lastLocalEdit.isAfter(lastSync) && lastLocalEdit.isAfter(remoteTimestamp)) || !hasLocalData) {
                logger.info("Detectados cambios locales más recientes. Subiendo datos a Supabase...");
                uploadAllData(userId);
                return;
            }

            logger.info("Los datos ya están sincronizados. No hay cambios pendientes.");

        } catch (Exception e) {
            logger.error("Error durante la sincronización: {}", e.getMessage(), e);
        }
    }

    /**
     * Descarga la colección completa de plataformas y ROMs desde Supabase,
     * eliminando antes la información local.
     */
    private static void downloadAllData(String userId) {
        try {
            String baseUrl = SecretsLoader.getSupabaseUrl();
            String key = SecretsLoader.getSupabaseKey();

            JSONArray plataformasJson =
                    getJsonArray(baseUrl + "/rest/v1/plataformas?user_id=eq." + userId + APIsConstants.SELECT_ALL, key);

            JSONArray romsJson =
                    getJsonArray(baseUrl + "/rest/v1/roms?user_id=eq." + userId + APIsConstants.SELECT_ALL, key);

            // Se elimina primero lo local para evitar conflictos
            romService.eliminarTodas();
            plataformaService.eliminarTodas();

            // Mapeo de IDs remotos → locales
            Map<String, Integer> idMap = new HashMap<>();

            for (Object o : plataformasJson) {
                JSONObject obj = (JSONObject) o;

                Plataforma p = new Plataforma();
                p.setNombre(obj.optString(APIsConstants.NOMBRE));
                p.setExtensionRom(obj.optString(APIsConstants.EXTENSION_ROM));
                p.setCarpeta(obj.optString(APIsConstants.CARPETA));

                plataformaService.guardar(p);
                idMap.put(obj.optString(APIsConstants.ID), p.getId());
            }

            for (Object o : romsJson) {
                JSONObject obj = (JSONObject) o;

                String remotePlatformId = obj.optString(APIsConstants.PLATAFORMA_ID);
                if (!idMap.containsKey(remotePlatformId)) continue;

                Rom r = new Rom();
                r.setTitulo(obj.optString(APIsConstants.TITULO));
                r.setDescripcion(obj.optString(APIsConstants.DESCRIPCION));
                r.setImagen(obj.optString(APIsConstants.IMAGEN));
                r.setFavorito(obj.optBoolean(APIsConstants.FAVORITO));
                r.setJugado(obj.optBoolean(APIsConstants.JUGADO));
                r.setRuta(obj.optString(APIsConstants.RUTA, null));

                r.setPlataforma(plataformaService.buscarPorId(idMap.get(remotePlatformId)));

                romService.guardar(r);
            }

            updateRemoteTimestamp(userId);
            SyncStateUtils.updateLastSync(userId);

            logger.info("Descarga finalizada: {} plataformas y {} ROMs sincronizadas.",
                    plataformasJson.length(), romsJson.length());

        } catch (Exception e) {
            logger.error("Error descargando datos desde Supabase", e);
        }
    }

    /**
     * Sube la colección local completa a Supabase. Antes elimina cualquier dato remoto previo.
     */
    private static void uploadAllData(String userId) {
        try {
            String baseUrl = SecretsLoader.getSupabaseUrl();
            String key = SecretsLoader.getSupabaseKey();

            borrarDatosRemotos(userId, baseUrl, key);

            List<Plataforma> plataformas = plataformaService.obtenerTodasConRoms();
            Map<Integer, String> remoteIds = new HashMap<>();

            // Primero se suben las plataformas
            for (Plataforma p : plataformas) {
                JSONObject json = new JSONObject()
                        .put(APIsConstants.NOMBRE, p.getNombre())
                        .put(APIsConstants.EXTENSION_ROM, p.getExtensionRom())
                        .put(APIsConstants.CARPETA, p.getCarpeta())
                        .put(APIsConstants.USER_ID, userId);

                JSONArray response = postJson(baseUrl + "/rest/v1/plataformas", key, json, true);

                if (response != null && !response.isEmpty()) {
                    String remoteId = response.getJSONObject(0).optString(APIsConstants.ID, null);
                    remoteIds.put(p.getId(), remoteId);

                } else {
                    logger.warn("Plataforma '{}' subida sin obtener ID remoto.", p.getNombre());
                }
            }

            // Ahora se suben las ROMs asociadas
            for (Plataforma p : plataformas) {
                String remotePlatId = remoteIds.get(p.getId());
                if (remotePlatId == null) {
                    logger.warn("Se omiten ROMs de la plataforma '{}' al no tener ID remoto.", p.getNombre());
                    continue;
                }

                for (Rom r : p.getRoms()) {
                    JSONObject json = new JSONObject()
                            .put(APIsConstants.TITULO, r.getTitulo())
                            .put(APIsConstants.DESCRIPCION, r.getDescripcion())
                            .put(APIsConstants.IMAGEN, r.getImagen())
                            .put(APIsConstants.FAVORITO, r.isFavorito())
                            .put(APIsConstants.JUGADO, r.isJugado())
                            .put(APIsConstants.RUTA, r.getRuta())
                            .put(APIsConstants.USER_ID, userId)
                            .put(APIsConstants.PLATAFORMA_ID, remotePlatId);

                    postJson(baseUrl + "/rest/v1/roms", key, json, false);
                }
            }

            updateRemoteTimestamp(userId);
            SyncStateUtils.updateLastSync(userId);

            logger.info("Subida completa a Supabase. Total plataformas: {}", plataformas.size());

        } catch (Exception e) {
            logger.error("Error subiendo datos a Supabase", e);
        }
    }

    /**
     * Ejecuta una petición GET y devuelve un JSONArray.
     */
    private static JSONArray getJsonArray(String url, String apiKey) throws IOException {
        HttpURLConnection conn = openConnection(url, APIsConstants.GET, apiKey);

        try (InputStream is = conn.getInputStream()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONArray(body);
        }
    }

    /**
     * Ejecuta una petición POST con JSON.
     */
    private static JSONArray postJson(String url, String apiKey, JSONObject json, boolean expectReturn) throws IOException {

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(APIsConstants.POST);
        conn.setRequestProperty(APIsConstants.APIKEY, apiKey);
        conn.setRequestProperty(APIsConstants.AUTHORIZATION,
                APIsConstants.BEARER_PREFIX + SupabaseAuthService.getAccessToken());
        conn.setRequestProperty(APIsConstants.CONTENT_TYPE, APIsConstants.CONTENT_TYPE_JSON);
        if (expectReturn) conn.setRequestProperty(APIsConstants.PREFER, APIsConstants.RETURN_REPRESENTATION);
        conn.setDoOutput(true);

        // Supabase requiere array JSON para inserciones múltiples
        String body = "[" + json.toString() + "]";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }

        int status = conn.getResponseCode();

        InputStream is = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

        String resp = "";
        if (is != null) {
            try (Scanner sc = new Scanner(is).useDelimiter("\\A")) {
                if (sc.hasNext()) resp = sc.next();
            }
        }

        if (status >= 200 && status < 300) {
            if (!resp.isBlank()) {
                return new JSONArray(resp);
            }
            return new JSONArray();
        }

        logger.error("Error en POST {}: HTTP {} – {}", url, status, resp);
        return new JSONArray();
    }

    /**
     * Crea una conexión HTTP básica configurada para Supabase.
     */
    private static HttpURLConnection openConnection(String url, String method, String apiKey) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty(APIsConstants.APIKEY, apiKey);
        conn.setRequestProperty(APIsConstants.AUTHORIZATION,
                APIsConstants.BEARER_PREFIX + SupabaseAuthService.getAccessToken());
        return conn;
    }

    /**
     * Elimina los datos remotos del usuario antes de subirlos de nuevo.
     */
    private static void borrarDatosRemotos(String userId, String baseUrl, String apiKey) throws IOException {
        for (String table : List.of("roms", "plataformas")) {
            HttpURLConnection conn =
                    openConnection(baseUrl + "/rest/v1/" + table + "?user_id=eq." + userId, APIsConstants.DELETE, apiKey);
            conn.getResponseCode(); // ejecución directa sin uso de respuesta
        }
    }

    /**
     * Recupera el timestamp remoto más reciente.
     * Si no existe registro, genera uno.
     */
    private static LocalDateTime getRemoteTimestamp(String userId) throws IOException {
        String url = SecretsLoader.getSupabaseUrl()
                + "/rest/v1/sync_status?user_id=eq." + userId + "&select=last_updated";

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("apikey", SecretsLoader.getSupabaseKey());
        conn.setRequestProperty("Authorization", "Bearer " + SupabaseAuthService.getAccessToken());

        String resp = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();
        JSONArray arr = new JSONArray(resp);

        if (arr.length() == 0) {
            updateRemoteTimestamp(userId);
            return LocalDateTime.MIN;
        }

        return LocalDateTime.parse(arr.getJSONObject(0).getString("last_updated"), FMT);
    }

    /**
     * Actualiza el timestamp remoto indicando la última modificación.
     */
    private static void updateRemoteTimestamp(String userId) throws IOException {
        JSONObject json = new JSONObject()
                .put("user_id", userId)
                .put("last_updated", LocalDateTime.now().format(FMT));

        postJson(SecretsLoader.getSupabaseUrl() + "/rest/v1/sync_status",
                SecretsLoader.getSupabaseKey(),
                json,
                false);
    }

    /**
     * Restaura manualmente los datos desde Supabase.
     */
    public static void restoreFromCloud() {
        String userId = SupabaseAuthService.getUserId();
        if (userId == null) {
            logger.warn("Restauración cancelada: no hay usuario autenticado.");
            return;
        }

        logger.info("Iniciando restauración desde Supabase para el usuario {}.", userId);
        downloadAllData(userId);
        logger.info("Restauración completada.");
    }
}
