package com.romraider.utils;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

public class SoundUtils {

    private static final Logger logger = LoggerFactory.getLogger(SoundUtils.class);
    private static final Random RANDOM = new Random();

    public static void playRandomSound() {
        try {
            URL resource = SoundUtils.class.getResource("/sounds/");
            if (resource == null) {
                logger.error("No se encontró la carpeta /sounds/");
                return;
            }

            File folder = new File(resource.toURI());
            File[] files = folder.listFiles((dir, name) ->
                    name.endsWith(".mp3") || name.endsWith(".wav")
            );

            if (files == null || files.length == 0) {
                logger.error("No hay sonidos en la carpeta /sounds/");
                return;
            }

            File randomFile = files[RANDOM.nextInt(files.length)];
            logger.error("Reproduciendo: " + randomFile.getName());

            Media sound = new Media(randomFile.toURI().toString());
            MediaPlayer player = new MediaPlayer(sound);
            player.setVolume(0.5);
            player.play();

        } catch (URISyntaxException e) {
            logger.error("Error al cargar sonidos: " + e.getMessage());
        }
    }
}
