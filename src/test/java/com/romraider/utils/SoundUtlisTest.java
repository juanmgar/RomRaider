package com.romraider.utils;

import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundUtilsTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {
            });
        } catch (IllegalStateException e) {
        }
    }

    @Test
    void play_returnsNull_whenSoundDoesNotExist() {
        String nonExistingSound = "this_sound_should_not_exist_12345";

        MediaPlayer player = SoundUtils.play(nonExistingSound);

        assertNull(player, "Se esperaba null cuando el sonido no existe");
    }
}
