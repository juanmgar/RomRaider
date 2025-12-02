package com.romraider.utils;

import com.romraider.api.APIsConstants;
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
 * Maneja el archivo de estado de sincronización con Supabase.
 * <p>
 * La información se almacena en un archivo JSON persistente en:
 * <pre>~/.romraider/sync_status.json</pre>
 *
 * Las claves registradas incluyen:
 * <ul>
 *     <li>user_id — usuario que realizó la última sincronización local</li>
 *     <li>last_local_user — usuario que realizó el último cambio local</li>
 *     <li>last_local_edit — fecha del último cambio local</li>
 *     <li>last_sync — fecha de la última sincronización ejecutada localmente</li>
 *     <li>last_remote_sync — última fecha de sincronización conocida en Supabase</li>
 * </ul>
 *
 * Las fechas se almacenan con formato ISO-8601.
 *
 * Esta utilidad garantiza escritura segura mediante uso de archivo temporal
 * y reemplazo atómico cuando es posible.
 */
public class SyncStateUtils {

    /**
     * Logger para registrar operaciones de lectura/escritura y cambios en el estado de sincronización.
     */
    private static final Logger logger = LoggerFactory.getLogger(SyncStateUtils.class);

    /**
     * Ruta al archivo JSON donde se almacena el estado.
     */
    private static final Path FILE =
            Paths.get(System.getProperty("user.home"), ".romraider", "sync_status.json");

    /**
     * Formato estándar ISO utilizado para guardar y leer fechas.
     */
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Lee el archivo JSON de estado.
     * Si no existe, lo crea con valores por defecto y lo devuelve.
     *
     * @return el objeto {@link JSONObject} cargado o generado.
     */
    private static JSONObject readJson() {
        try {
            if (Files.exists(FILE)) {
                return new JSONObject(Files.readString(FILE));
            } else {
                logger.info("Archivo sync_status.json no encontrado. Creando uno nuevo...");

                JSONObject init = new JSONObject()
                        .put(APIsConstants.USER_ID, "")
                        .put(APIsConstants.LAST_LOCAL_USER, "")
                        .put(APIsConstants.LAST_LOCAL_EDIT, LocalDateTime.MIN.format(FMT))
                        .put(APIsConstants.LAST_SYNC, LocalDateTime.MIN.format(FMT))
                        .put(APIsConstants.LAST_REMOTE_SYNC, LocalDateTime.MIN.format(FMT));

                writeJson(init);
                return init;
            }
        } catch (IOException e) {
            logger.error("Error leyendo sync_status.json: {}", e.getMessage());
            return new JSONObject();
        }
    }

    /**
     * Escribe el JSON al disco de manera segura creando previamente un archivo temporal
     * y moviéndolo al destino final mediante reemplazo atómico.
     *
     * @param obj objeto JSON a persistir.
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

    /**
     * Obtiene el usuario que ejecutó la última sincronización local.
     *
     * @return user_id o cadena vacía si no existe.
     */
    public static String getLastUser() {
        return readJson().optString(APIsConstants.USER_ID, "");
    }

    /**
     * Obtiene el usuario que realizó el último cambio local en datos.
     *
     * @return nombre de usuario o cadena vacía.
     */
    public static String getLastLocalUser() {
        return readJson().optString(APIsConstants.LAST_LOCAL_USER, "");
    }

    /**
     * Obtiene la fecha y hora del último cambio local.
     *
     * @return {@link LocalDateTime} o {@link LocalDateTime#MIN} si no existe valor.
     */
    public static LocalDateTime getLastLocalEdit() {
        return parseTime(APIsConstants.LAST_LOCAL_EDIT);
    }

    /**
     * Obtiene la fecha de la última sincronización realizada localmente.
     */
    public static LocalDateTime getLastSync() {
        return parseTime(APIsConstants.LAST_SYNC);
    }

    /**
     * Obtiene la última fecha de sincronización remota conocida
     * (reportada desde Supabase).
     */
    public static LocalDateTime getLastRemoteSync() {
        return parseTime(APIsConstants.LAST_REMOTE_SYNC);
    }

    /**
     * Marca que ocurrió un cambio local.
     * Actualiza:
     * <ul>
     *     <li>last_local_edit → fecha actual</li>
     *     <li>last_local_user → usuario actual o "" si sin sesión</li>
     * </ul>
     */
    public static void markLocalChange() {
        JSONObject obj = readJson();

        obj.put(APIsConstants.LAST_LOCAL_EDIT, LocalDateTime.now().format(FMT));

        String userId = SupabaseAuthService.getUserId();
        obj.put(APIsConstants.LAST_LOCAL_USER, userId != null ? userId : "");

        writeJson(obj);
    }

    /**
     * Actualiza el usuario y la fecha de la última sincronización ejecutada localmente.
     *
     * @param userId identificador del usuario autenticado.
     */
    public static void updateLastSync(String userId) {
        JSONObject obj = readJson();
        obj.put(APIsConstants.USER_ID, userId);
        obj.put(APIsConstants.LAST_SYNC, LocalDateTime.now().format(FMT));

        logger.info("Actualizando last_sync y user_id en sync_status.json");

        writeJson(obj);
    }

    /**
     * Actualiza la fecha de sincronización remota almacenada localmente.
     *
     * @param timestamp fecha reportada desde Supabase.
     */
    public static void updateLastRemoteSync(LocalDateTime timestamp) {
        updateField(APIsConstants.LAST_REMOTE_SYNC, timestamp);
    }

    /**
     * Actualiza un campo concreto del JSON usando el formato de fecha estándar.
     *
     * @param key   clave del campo a modificar.
     * @param value valor temporal a registrar.
     */
    private static void updateField(String key, LocalDateTime value) {
        JSONObject obj = readJson();
        obj.put(key, value.format(FMT));

        logger.debug("Campo '{}' actualizado a {}", key, value);

        writeJson(obj);
    }

    /**
     * Convierte una fecha almacenada como texto dentro del JSON
     * a un objeto {@link LocalDateTime}.
     * Si el valor está vacío, devuelve {@link LocalDateTime#MIN}.
     *
     * @param key clave del campo temporal.
     * @return fecha convertida o {@link LocalDateTime#MIN} si no existe.
     */
    private static LocalDateTime parseTime(String key) {
        String val = readJson().optString(key, "");
        return val.isEmpty() ? LocalDateTime.MIN : LocalDateTime.parse(val, FMT);
    }
}
