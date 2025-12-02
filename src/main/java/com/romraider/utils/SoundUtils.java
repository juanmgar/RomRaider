package com.romraider.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * Utilidad para reproducir sonidos internos de la aplicación.
 * <p>
 * Los sonidos deben estar ubicados en el directorio de recursos:
 * <pre>/resources/sounds/</pre>
 * y se buscarán por nombre probando extensiones soportadas.
 * <p>
 * Admite automáticamente los formatos:
 * <ul>
 *     <li>.mp3</li>
 *     <li>.wav</li>
 * </ul>
 * El método {@link #play(String)} busca el sonido por nombre
 * con cualquiera de esas extensiones y reproduce el primero que encuentre.
 */
public class SoundUtils {

    /**
     * Logger para registrar información y errores relacionados
     * con la reproducción de sonidos.
     */
    private static final Logger logger = LoggerFactory.getLogger(SoundUtils.class);

    /**
     * Nombre estándar del sonido de inicio de la aplicación.
     */
    public static final String STARTUP = "startup";

    /**
     * Nombre estándar del sonido de alerta genérica.
     */
    public static final String ALERT = "alert";

    /**
     * Nombre estándar del sonido asociado a subidas/cargas (upload).
     */
    public static final String UPLOAD = "upload";

    /**
     * Reproduce un sonido dado su nombre, buscando automáticamente
     * entre los formatos soportados ({@code .mp3} y {@code .wav}).
     * <p>
     * El método recorre las extensiones soportadas en orden y utiliza
     * el primer recurso que encuentre bajo el path {@code /sounds/}.
     *
     * @param soundName nombre del sonido SIN extensión
     *                  (por ejemplo: {@link SoundUtils#ALERT}, {@code "alert"}).
     * @return un {@link MediaPlayer} si la reproducción fue iniciada correctamente,
     *         o {@code null} si ocurrió algún error o no se encontró el recurso.
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
