package com.romraider.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Utilidad para reproducir sonidos internos de la aplicación.
 *
 * Los sonidos deben estar ubicados en el directorio:
 * <pre>/resources/sounds/</pre>
 *
 * Admite automáticamente los formatos:
 * - .mp3
 * - .wav
 *
 * El método {@link #play(String)} busca el sonido por nombre
 * con cualquiera de esas extensiones y reproduce el primero que encuentre.
 */
public class SoundUtils {

    private static final Logger logger = LoggerFactory.getLogger(SoundUtils.class);

    // Nombres estándar usados en la interfaz
    public static final String STARTUP = "startup";
    public static final String ALERT = "alert";
    public static final String UPLOAD = "upload";

    /**
     * Reproduce un sonido dado su nombre, buscando automáticamente
     * entre los formatos soportados (.mp3 y .wav).
     *
     * @param soundName nombre del sonido SIN extensión
     *                  (ej.: SoundUtils.ALERT, "alert")
     *
     * @return un {@link MediaPlayer} si la reproducción fue iniciada correctamente,
     *         o null si ocurrió algún error o no se encontró el recurso.
     */
    public static MediaPlayer play(String soundName) {
        try {
            String[] extensions = {".mp3", ".wav"};
            URL soundUrl = null;

            /*
             * Intentamos localizar el archivo probando extensiones
             * soportadas en orden. Se detiene al encontrar la primera válida.
             */
            for (String ext : extensions) {
                soundUrl = SoundUtils.class.getResource("/sounds/" + soundName + ext);
                if (soundUrl != null) break;
            }

            // Si ningún archivo existe, se notifica por log
            if (soundUrl == null) {
                logger.warn("No se encontró el sonido solicitado: {}", soundName);
                return null;
            }

            // Crear y reproducir el sonido
            Media media = new Media(soundUrl.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);

            // Volumen por defecto: 70%
            player.setVolume(0.7);
            player.play();

            logger.info("Reproduciendo sonido: {}", soundName);
            return player;

        } catch (Exception e) {
            logger.error("Error al reproducir el sonido '{}': {}", soundName, e.getMessage());
            return null;
        }
    }
}
