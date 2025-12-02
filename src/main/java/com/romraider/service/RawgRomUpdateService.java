package com.romraider.service;

import com.romraider.api.RawgApiClient;
import com.romraider.model.Rom;
import com.romraider.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio encargado de actualizar una ROM con información obtenida
 * desde la API pública de RAWG.io.
 *
 * <p>Este servicio:</p>
 * <ul>
 *     <li>Solicita datos a RAWG.io (descripción e imagen).</li>
 *     <li>Actualiza los campos de la entidad {@link Rom}.</li>
 *     <li>Gestiona la descarga y guardado local de la imagen.</li>
 *     <li>Opcionalmente persiste los cambios mediante {@link RomService}.</li>
 * </ul>
 *
 * <p>El servicio también encapsula el manejo de errores y condiciones como:
 * descripciones demasiado largas, falta de datos en RAWG.io o fallos de red.</p>
 */
public class RawgRomUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(RawgRomUpdateService.class);

    /**
     * Longitud máxima permitida para la descripción descargada desde RAWG.io.
     * Esto evita problemas con campos limitados en la base de datos.
     */
    private static final int MAX_DESC_LENGTH = 3999;

    private final RomService romService;

    /**
     * Crea un servicio de actualización RAWG.io asociado a un {@link RomService}.
     *
     * @param romService servicio usado para persistir cambios cuando {@code persist = true}.
     */
    public RawgRomUpdateService(RomService romService) {
        this.romService = romService;
    }

    /**
     * Estados posibles tras intentar actualizar una ROM.
     */
    public enum Status {
        UPDATED,
        NOT_FOUND,
        ERROR
    }

    /**
     * Resultado detallado de una actualización RAWG.io.
     * Incluye un estado y, opcionalmente, un mensaje de error.
     */
    public static class UpdateResult {
        private final Status status;
        private final String errorMessage;

        /**
         * Crea un resultado de actualización.
         *
         * @param status       estado final de la operación.
         * @param errorMessage mensaje de error o null si no hubo.
         */
        public UpdateResult(Status status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }

        /**
         * @return estado final de la operación.
         */
        public Status getStatus() {
            return status;
        }

        /**
         * @return mensaje de error, o null si no hubo.
         */
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Llama a RAWG.io para obtener datos relacionados con la ROM dada,
     * actualizando su descripción e imagen.
     *
     * <p>El proceso incluye:</p>
     * <ul>
     *     <li>Consulta a RAWG.io mediante {@link RawgApiClient#obtenerInfo(String)}.</li>
     *     <li>Actualización de la descripción (truncando si excede {@link #MAX_DESC_LENGTH}).</li>
     *     <li>Descarga y guardado local de la imagen usando {@link ImageUtils}.</li>
     *     <li>Persistencia opcional en base de datos si {@code persist = true}.</li>
     * </ul>
     *
     * @param rom     la ROM a actualizar.
     * @param persist si es {@code true}, la ROM se guardará automáticamente mediante {@link RomService#guardar(Rom)}.
     * @return un {@link UpdateResult} indicando éxito, no encontrado o error.
     */
    public UpdateResult updateRomFromRawg(Rom rom, boolean persist) {
        try {
            logger.info("Solicitando datos RAWG.io para '{}'", rom.getTitulo());
            RawgApiClient.RomInfo info = RawgApiClient.obtenerInfo(rom.getTitulo());

            if (info == null || (info.descripcion == null && info.imageUrl == null)) {
                logger.warn("Sin datos en RAWG.io para '{}'", rom.getTitulo());
                return new UpdateResult(Status.NOT_FOUND, "No data found on RAWG.io");
            }

            // Descripción (con truncado)
            if (info.descripcion != null) {
                String desc = info.descripcion;
                if (desc.length() > MAX_DESC_LENGTH) {
                    logger.warn("Description over {} chars for '{}', truncating.", MAX_DESC_LENGTH, rom.getTitulo());
                    desc = desc.substring(0, MAX_DESC_LENGTH);
                }
                rom.setDescripcion(desc);
                logger.info("Descripción actualizada para '{}'", rom.getTitulo());
            }

            // Imagen
            if (info.imageUrl != null) {
                try {
                    String localPath = ImageUtils.downloadAndSaveImage(
                            info.imageUrl,
                            rom.getTitulo(),
                            rom.getId()
                    );
                    if (localPath != null) {
                        rom.setImagen(localPath);
                        logger.info("Imagen actualizada para '{}'", rom.getTitulo());
                    }
                } catch (Exception e) {
                    logger.error("Failed downloading image for '{}'", rom.getTitulo(), e);
                    // Se registra, pero no interrumpe el proceso global.
                }
            }

            if (persist) {
                romService.guardar(rom);
                logger.info("ROM '{}' guardada tras actualización RAWG.io", rom.getTitulo());
            }

            return new UpdateResult(Status.UPDATED, null);

        } catch (Exception e) {
            logger.error("Error actualizando '{}'", rom.getTitulo(), e);
            return new UpdateResult(Status.ERROR, e.getMessage());
        }
    }
}
