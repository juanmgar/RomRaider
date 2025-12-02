package com.romraider.utils;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Utilidades para gestionar imágenes asociadas a ROMs.
 *
 * Incluye funciones para:
 *  - Copiar imágenes locales a la carpeta interna de la aplicación
 *  - Descargar y guardar imágenes desde URL (RAWG.io u otras fuentes)
 *  - Cargar una imagen desde disco, con fallback a una imagen por defecto
 *
 * Todas las imágenes se guardan en:
 * <pre>
 *   ~/.romraider/images
 * </pre>
 */
public class ImageUtils {

    /**
     * Carpeta interna donde se almacenan las imágenes de ROMs.
     * Se crea automáticamente si no existe.
     */
    public static final File IMAGES_FOLDER =
            new File(System.getProperty("user.home"), ".romraider/images");

    /**
     * Copia una imagen local a la carpeta interna de ROM Raider.
     * Se genera un nombre seguro basado en el título de la ROM.
     *
     * @param sourceImage imagen seleccionada por el usuario
     * @param title título de la ROM (usado para generar nombre del archivo)
     * @return ruta absoluta del archivo copiado
     * @throws IOException si ocurre un error al copiar la imagen
     */
    public static String copyImageToLocalFolder(File sourceImage, String title) throws IOException {

        // Extraer extensión original del archivo
        String extension = sourceImage.getName()
                .substring(sourceImage.getName().lastIndexOf('.') + 1);

        // Título seguro para usar en un nombre de archivo
        String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_");

        // Se añade timestamp para evitar colisiones de nombres
        String filename = safeTitle + "_" + System.currentTimeMillis() + "." + extension;

        // Asegurar que la carpeta existe
        IMAGES_FOLDER.mkdirs();

        File destFile = new File(IMAGES_FOLDER, filename);

        // Copia física del archivo
        Files.copy(sourceImage.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return destFile.getAbsolutePath();
    }

    /**
     * Descarga una imagen desde una URL y la guarda en la carpeta interna.
     * El nombre del archivo incluye el ID de la ROM para evitar conflictos.
     *
     * @param imageUrl URL completa de la imagen a descargar
     * @param title título de la ROM (usado para nombrar el archivo)
     * @param romId identificador único de la ROM
     * @return ruta absoluta del archivo guardado
     * @throws IOException si ocurre un error al descargar o guardar el archivo
     */
    public static String downloadAndSaveImage(String imageUrl, String title, int romId) throws IOException {

        String extension = imageUrl.substring(imageUrl.lastIndexOf('.') + 1);
        String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_");

        String filename = safeTitle + "_" + romId + "." + extension;

        IMAGES_FOLDER.mkdirs();
        File destFile = new File(IMAGES_FOLDER, filename);

        // Descargar mediante stream desde URL
        try (InputStream in = new URL(imageUrl).openStream()) {
            Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return destFile.getAbsolutePath();
    }

    /**
     * Elimina una imagen del sistema de archivos si existe y es un archivo regular.
     * <p>
     * Si la ruta es {@code null}, está en blanco, o el archivo no existe,
     * no se realiza ninguna acción y se devuelve {@code false}.
     *
     * @param imagePath ruta absoluta de la imagen a eliminar.
     * @return {@code true} si el archivo existía y se ha eliminado correctamente;
     *         {@code false} en caso contrario.
     */
    public static boolean deleteImageIfExists(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return false;
        }

        File file = new File(imagePath);

        if (file.exists() && file.isFile()) {
            return file.delete();
        }

        return false;
    }

}
