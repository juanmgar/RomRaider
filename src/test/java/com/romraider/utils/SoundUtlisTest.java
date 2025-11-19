package com.romraider.utils;

import javafx.scene.media.MediaPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class SoundUtilsTest {

    @Test
    void play_returnsNull_whenSoundDoesNotExist() {
        // Nombre imposible para asegurarnos de que no existe
        String nonExistingSound = "this_sound_should_not_exist_12345";

        MediaPlayer player = SoundUtils.play(nonExistingSound);

        // Como no se encuentra el recurso, play() debe devolver null
        assertNull(player, "Se esperaba null cuando el sonido no existe");
    }
}
