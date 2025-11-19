package com.romraider.utils;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilsTest {

    @Test
    void copyImageToLocalFolder_copiesFileAndReturnsPath() throws IOException {
        // Arrange: crear un "fichero imagen" temporal
        Path tempFile = Files.createTempFile("test-image", ".png");
        Files.write(tempFile, new byte[]{1, 2, 3}); // contenido dummy

        File sourceImage = tempFile.toFile();
        String title = "Super Mario Bros. (EU)";

        // Act
        String resultPath = ImageUtils.copyImageToLocalFolder(sourceImage, title);

        // Assert
        assertNotNull(resultPath);
        File destFile = new File(resultPath);

        assertTrue(destFile.exists(), "El fichero de destino no existe");
        assertEquals(
                ImageUtils.IMAGES_FOLDER.getAbsolutePath(),
                destFile.getParentFile().getAbsolutePath(),
                "La imagen no se ha copiado en la carpeta IMAGES_FOLDER"
        );

        String fileName = destFile.getName();
        assertTrue(
                fileName.matches("Super_Mario_Bros_+EU_+\\d+\\.png"),
                "El nombre de archivo no cumple el patrón esperado"
        );
        
    }

    @Test
    void deleteImageIfExists_returnsTrueWhenFileExists() throws IOException {
        // Arrange
        Path tempFile = Files.createTempFile("image-to-delete", ".png");
        File file = tempFile.toFile();
        assertTrue(file.exists(), "El fichero de prueba debería existir");

        // Act
        boolean deleted = ImageUtils.deleteImageIfExists(file.getAbsolutePath());

        // Assert
        assertTrue(deleted, "Se esperaba true al borrar un fichero existente");
        assertFalse(file.exists(), "El fichero debería haberse borrado");
    }

    @Test
    void deleteImageIfExists_returnsFalseWhenPathIsNullOrBlank() {
        assertFalse(ImageUtils.deleteImageIfExists(null));
        assertFalse(ImageUtils.deleteImageIfExists(""));
        assertFalse(ImageUtils.deleteImageIfExists("   "));
    }

    @Test
    void deleteImageIfExists_returnsFalseWhenFileDoesNotExist() {
        String fakePath = new File(ImageUtils.IMAGES_FOLDER, "no_existo_123.png").getAbsolutePath();
        assertFalse(ImageUtils.deleteImageIfExists(fakePath));
    }
}
