package com.romraider.service;

import com.romraider.api.RawgApiClient;
import com.romraider.model.Rom;
import com.romraider.utils.ImageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawgRomUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(RawgRomUpdateService.class);
    private static final int MAX_DESC_LENGTH = 3999;

    private final RomService romService;

    public RawgRomUpdateService(RomService romService) {
        this.romService = romService;
    }

    public enum Status {
        UPDATED,
        NOT_FOUND,
        ERROR
    }

    public static class UpdateResult {
        private final Status status;
        private final String errorMessage;

        public UpdateResult(Status status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public Status getStatus() {
            return status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Llama a RAWG.io, actualiza descripción e imagen en la ROM y opcionalmente persiste en BD.
     *
     * @param rom     ROM a actualizar.
     * @param persist si true, guarda la ROM con romService.guardar(rom).
     * @return resultado con estado y mensaje de error (si aplica).
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
                    // No consideramos esto un NOT_FOUND, pero sí lo dejamos en logs
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
