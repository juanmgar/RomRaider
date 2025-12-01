package com.romraider.utils;

import com.romraider.api.SupabaseAuthService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Maneja el estado de sincronización con Supabase.
 * <p>
 * Guarda en un JSON:
 * - user_id: usuario que realizó la última sync
 * - last_local_user: usuario que realizó el último cambio local
 * - last_local_edit: fecha del último cambio local
 * - last_sync: fecha de la última sincronización local
 * - last_remote_sync: última fecha conocida en remoto
 */
public class SyncStateUtils {

    private static final Logger logger = LoggerFactory.getLogger(SyncStateUtils.class);

    private static final Path FILE =
            Paths.get(System.getProperty("user.home"), ".romraider", "sync_status.json");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Lee el archivo JSON de estado. Si no existe, lo crea con valores iniciales.
     */
    private static JSONObject readJson() {
        try {
            if (Files.exists(FILE)) {
                return new JSONObject(Files.readString(FILE));
            } else {
                logger.info("Archivo sync_status.json no encontrado. Creando uno nuevo...");

                JSONObject init = new JSONObject()
                        .put("user_id", "")
                        .put("last_local_user", "")
                        .put("last_local_edit", LocalDateTime.MIN.format(FMT))
                        .put("last_sync", LocalDateTime.MIN.format(FMT))
                        .put("last_remote_sync", LocalDateTime.MIN.format(FMT));

                writeJson(init);
                return init;
            }
        } catch (IOException e) {
            logger.error("Error leyendo sync_status.json: {}", e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * Escribe el JSON al disco de manera segura usando un archivo temporal.
     */
    private static void writeJson(JSONObject obj) {
        try {
            Files.createDirectories(FILE.getParent());

            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(tmp, obj.toString(2));
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            logger.error("Error escribiendo sync_status.json: {}", e.getMessage());
        }
    }

    public static String getLastUser() {
        return readJson().optString("user_id", "");
    }

    public static String getLastLocalUser() {
        return readJson().optString("last_local_user", "");
    }

    public static LocalDateTime getLastLocalEdit() {
        return parseTime("last_local_edit");
    }

    public static LocalDateTime getLastSync() {
        return parseTime("last_sync");
    }

    public static LocalDateTime getLastRemoteSync() {
        return parseTime("last_remote_sync");
    }

    /**
     * Marca que hubo un cambio local.
     * Se registra también el usuario actual (o "" si es offline).
     */
    public static void markLocalChange() {
        JSONObject obj = readJson();

        obj.put("last_local_edit", LocalDateTime.now().format(FMT));

        String userId = SupabaseAuthService.getUserId();
        obj.put("last_local_user", userId != null ? userId : "");

        writeJson(obj);
    }

    /**
     * Actualiza la fecha de sincronización local (después de un sync completo).
     *
     * @param userId usuario autenticado
     */
    public static void updateLastSync(String userId) {
        JSONObject obj = readJson();
        obj.put("user_id", userId);
        obj.put("last_sync", LocalDateTime.now().format(FMT));

        logger.info("Actualizando last_sync y user_id en sync_status.json");

        writeJson(obj);
    }

    /**
     * Actualiza la fecha de sincronización remota.
     */
    public static void updateLastRemoteSync(LocalDateTime timestamp) {
        updateField("last_remote_sync", timestamp);
    }

    /**
     * Actualiza un campo concreto del JSON.
     */
    private static void updateField(String key, LocalDateTime value) {
        JSONObject obj = readJson();
        obj.put(key, value.format(FMT));

        logger.debug("Campo '{}' actualizado a {}", key, value);

        writeJson(obj);
    }

    /**
     * Convierte una fecha guardada en texto a LocalDateTime.
     */
    private static LocalDateTime parseTime(String key) {
        String val = readJson().optString(key, "");
        return val.isEmpty() ? LocalDateTime.MIN : LocalDateTime.parse(val, FMT);
    }
}
