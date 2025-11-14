package com.romraider.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;


public class SoundUtils {

    private static final Logger logger = LoggerFactory.getLogger(SoundUtils.class);

    public static final String STARTUP = "startup";
    public static final String ALERT = "alert";

    public static MediaPlayer play(String soundName) {
        try {
            String[] extensions = {".mp3", ".wav"};
            URL soundUrl = null;

            for (String ext : extensions) {
                soundUrl = SoundUtils.class.getResource("/sounds/" + soundName + ext);
                if (soundUrl != null) break;
            }

            if (soundUrl == null) {
                logger.warn("No se encontró el sonido: {}", soundName);
                return null;
            }

            Media media = new Media(soundUrl.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
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
