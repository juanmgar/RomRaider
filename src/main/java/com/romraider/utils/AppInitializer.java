package com.romraider.utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class AppInitializer {

    private static final String APP_NAME = "romraider";
    private static final String CONFIG_FILE_NAME = "romraider.properties";

    private static Path baseDir;
    public static Path configDir;
    public static Path dbDir;
    public static Path logDir;
    public static Path configFile;

    public static void initialize() {
        // Establece el directorio base según sistema operativo
        String userHome = System.getProperty("user.home");
        baseDir = Paths.get(userHome, "." + APP_NAME);

        configDir = baseDir.resolve("config");
        dbDir = baseDir.resolve("db");
        logDir = baseDir.resolve("logs");
        configFile = configDir.resolve(CONFIG_FILE_NAME);

        try {
            Files.createDirectories(configDir);
            Files.createDirectories(dbDir);
            Files.createDirectories(logDir);

            // Copia config por defecto si no existe
            if (Files.notExists(configFile)) {
                copyDefaultConfig();
            }

            System.out.println("Directorio de la app: " + baseDir.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error inicializando estructura de carpetas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void copyDefaultConfig() throws IOException {
        try (InputStream in = AppInitializer.class.getResourceAsStream("/config/default-config.properties")) {
            if (in == null) {
                throw new FileNotFoundException("Archivo default-config.properties no encontrado en resources");
            }
            Files.copy(in, configFile);
            System.out.println("Archivo de configuración creado: " + configFile);
        }
    }

    public static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Error cargando archivo de configuración: " + e.getMessage());
        }
        return props;
    }
}
