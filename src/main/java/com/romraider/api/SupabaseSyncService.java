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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Servicio responsable de sincronizar la colección de plataformas y ROMs
 * entre la base de datos local y el backend remoto alojado en Supabase.
 *
 * <p>Este servicio implementa una sincronización bidireccional controlada mediante
 * timestamps, protegiendo contra sobrescrituras involuntarias
 * (especialmente al cambiar de usuario local).</p>
 *
 * <p>Principales funciones:</p>
 * <ul>
 *     <li>Comparación de timestamps local/remoto</li>
 *     <li>Descarga completa de datos (pull)</li>
 *     <li>Subida completa de datos (push)</li>
 *     <li>Asociación de datos por usuario (user_id)</li>
 *     <li>Evitar sobrescrituras entre usuarios distintos</li>
 *     <li>Control de la última edición local y última sincronización</li>
 * </ul>
 *
 * <p>Todo el tráfico se realiza mediante REST usando PostgREST + políticas RLS de Supabase.</p>
 */
public class SupabaseSyncService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseSyncService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

    /** Servicios locales de acceso a BD */
    private static final PlataformaService plataformaService = new PlataformaService();
    private static final RomService romService = new RomService();

    /**
     * Punto principal de sincronización.
     *
     * <p>Decide automáticamente si:</p>
     * <ul>
     *     <li>Descargar datos desde Supabase</li>
     *     <li>Subir datos al servidor</li>
     *     <li>No realizar ninguna acción</li>
     * </ul>
     *
     * <p>La decisión se basa en:</p>
     * <ul>
     *     <li>Última sincronización local</li>
     *     <li>Última edición local</li>
     *     <li>Timestamp remoto</li>
     *     <li>Cambio de usuario respecto a la última sesión</li>
     * </ul>
     *
     * <p>También evita un caso crítico: subir cambios locales hechos mientras
     * estaba logueado otro usuario, lo cual sobrescribiría datos remotos ajenos.</p>
     */
    public static void syncWithSupabase() {
        String userId = SupabaseAuthService.getUserId();
        String lastLocalUser = SyncStateUtils.getLastLocalUser();

        if (userId == null) {
            logger.warn("No hay usuario autenticado. Sincronización omitida.");
            return;
        }

        try {
            boolean firstSync = SyncStateUtils.getLastUser() == null
                    || !userId.equals(SyncStateUtils.getLastUser());

            boolean hasLocalData = !plataformaService.obtenerTodas().isEmpty();

            // Primera sincronización o cambio de usuario -> descargar datos remotos
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
                    "Estado de timestamps -> última sync local: {}, última edición local: {}, remoto: {}",
                    lastSync, lastLocalEdit, remoteTimestamp
            );

            // Caso 1 -> Remoto tiene datos más recientes
            if (remoteTimestamp.isAfter(lastSync) && remoteTimestamp.isAfter(lastLocalEdit)) {
                logger.info("Detectados datos más recientes en Supabase. Descargando...");
                downloadAllData(userId);
                return;
            }

            // Caso 2 -> Local tiene datos nuevos y pertenecen al mismo usuario
            if (lastLocalEdit.isAfter(lastSync) && lastLocalEdit.isAfter(remoteTimestamp)) {

                // Protección anti-sobrescritura entre usuarios
                if (!userId.equals(lastLocalUser)) {
                    logger.info("Cambios locales detectados, pero NO pertenecen al usuario {} (lastLocalUser='{}'). "
                                    + "NO se suben para evitar sobrescribir datos remotos.",
                            userId, lastLocalUser);
                    downloadAllData(userId);
                    return;
                }

                logger.info("Cambios locales pertenecen al usuario {}. Subiendo datos a Supabase...", userId);
                uploadAllData(userId);
                return;
            }

            // Caso 3 -> No hay datos locales -> se suben los datos remotos (flujo original)
            if (!hasLocalData) {
                logger.info("Base local vacía. Subiendo datos a Supabase...");
                uploadAllData(userId);
                return;
            }

            // Caso 4 -> Todo sincronizado
            logger.info("Los datos ya están sincronizados. No hay cambios pendientes.");

        } catch (Exception e) {
            logger.error("Error durante la sincronización: {}", e.getMessage(), e);
        }
    }

    /**
     * Descarga toda la información del usuario desde Supabase y sobrescribe la BD local.
     *
     * <p>Pasos:</p>
     * <ol>
     *     <li>Descargar plataformas</li>
     *     <li>Descargar ROMs</li>
     *     <li>Eliminar contenido local</li>
     *     <li>Reconstruir IDs locales manteniendo la integridad relacional</li>
     *     <li>Actualizar timestamp remoto y local</li>
     * </ol>
     */
    private static void downloadAllData(String userId) {
        try {
            String baseUrl = SecretsLoader.getSupabaseUrl();
            String key = SecretsLoader.getSupabaseKey();

            JSONArray plataformasJson =
                    getJsonArray(baseUrl + "/rest/v1/plataformas?user_id=eq." + userId + APIsConstants.SELECT_ALL, key);

            JSONArray romsJson =
                    getJsonArray(baseUrl + "/rest/v1/roms?user_id=eq." + userId + APIsConstants.SELECT_ALL, key);

            romService.eliminarTodas();
            plataformaService.eliminarTodas();

            Map<String, Integer> idMap = new HashMap<>();

            // Importar plataformas
            for (Object o : plataformasJson) {
                JSONObject obj = (JSONObject) o;

                Plataforma p = new Plataforma();
                p.setNombre(obj.optString(APIsConstants.NOMBRE));
                p.setExtensionRom(obj.optString(APIsConstants.EXTENSION_ROM));
                p.setCarpeta(obj.optString(APIsConstants.CARPETA));

                plataformaService.guardar(p);
                idMap.put(obj.optString(APIsConstants.ID), p.getId());
            }

            // Importar ROMs asociadas
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
     * Sube toda la colección local de plataformas y ROMs al backend remoto.
     *
     * <p>Antes de subir, ejecuta:</p>
     * <ul>
     *     <li>Eliminación de datos remotos del usuario</li>
     *     <li>Inserción de plataformas, recuperando IDs generados por Supabase</li>
     *     <li>Inserción de ROMs vinculadas mediante los IDs remotos nuevos</li>
     * </ul>
     */
    private static void uploadAllData(String userId) {
        try {
            String baseUrl = SecretsLoader.getSupabaseUrl();
            String key = SecretsLoader.getSupabaseKey();

            borrarDatosRemotos(userId, baseUrl, key);

            List<Plataforma> plataformas = plataformaService.obtenerTodasConRoms();
            Map<Integer, String> remoteIds = new HashMap<>();

            // Subida de plataformas
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

            // Subida de ROMs
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
     * Realiza una petición GET a una tabla Supabase y devuelve un JSONArray.
     *
     * @param url  URL del endpoint Supabase
     * @param apiKey key pública del proyecto
     */
    private static JSONArray getJsonArray(String url, String apiKey) throws IOException {
        HttpURLConnection conn = openConnection(url, APIsConstants.GET, apiKey);

        try (InputStream is = conn.getInputStream()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return new JSONArray(body);
        }
    }

    /**
     * Ejecuta una petición POST a Supabase enviando un body JSON con formato array.
     *
     * @param expectReturn true si se espera un array con la fila creada (Prefer: return=representation)
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

        // Supabase requiere array JSON para inserciones bulk
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
     * Abre una conexión HTTP a Supabase configurada con API key y token de usuario.
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
     * Elimina toda la información remota del usuario antes de subir la versión local.
     *
     * <p>Se borran primero ROMs y luego plataformas para respetar las FKs.</p>
     */
    private static void borrarDatosRemotos(String userId, String baseUrl, String apiKey) throws IOException {
        for (String table : List.of("roms", "plataformas")) {
            HttpURLConnection conn =
                    openConnection(baseUrl + "/rest/v1/" + table + "?user_id=eq." + userId, APIsConstants.DELETE, apiKey);
            conn.getResponseCode(); // Fuerza ejecución
        }
    }

    /**
     * Obtiene el timestamp remoto más reciente del usuario.
     * Si no existe registro, lo crea automáticamente.
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
     * Actualiza el timestamp remoto indicando que el usuario tiene cambios nuevos.
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
     * Fuerza una restauración manual de los datos remotos ignorando la lógica de sincronización.
     *
     * <p>Equivalente a "Replace local database with server version".</p>
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
