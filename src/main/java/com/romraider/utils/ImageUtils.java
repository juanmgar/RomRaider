package com.romraider.utils;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ImageUtils {

    public static final File IMAGES_FOLDER = new File(System.getProperty("user.home"), ".romraider/images");

    public static String copyImageToLocalFolder(File sourceImage, String title) throws IOException {
        String extension = sourceImage.getName().substring(sourceImage.getName().lastIndexOf('.') + 1);
        String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
        String filename = safeTitle + "_" + System.currentTimeMillis() + "." + extension;

        IMAGES_FOLDER.mkdirs();
        File destFile = new File(IMAGES_FOLDER, filename);
        Files.copy(sourceImage.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return destFile.getAbsolutePath();
    }

    public static String downloadAndSaveImage(String imageUrl, String title, int romId) throws IOException {
        String extension = imageUrl.substring(imageUrl.lastIndexOf('.') + 1);
        String safeTitle = title.replaceAll("[^a-zA-Z0-9]", "_");
        String filename = safeTitle + "_" + romId + "." + extension;

        IMAGES_FOLDER.mkdirs();
        File destFile = new File(IMAGES_FOLDER, filename);

        try (InputStream in = new URL(imageUrl).openStream()) {
            Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return destFile.getAbsolutePath();
    }

    public static Image loadRomImageOrDefault(String path) {
        File imageFile = new File(path);
        if (imageFile.exists()) {
            return new Image("file:" + imageFile.getAbsolutePath(), true);
        } else {
            return new Image(ImageUtils.class.getResourceAsStream("/assets/no-image.png"));
        }
    }
}
