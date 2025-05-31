package com.romraider.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

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

            logger.info("App directories initialized at: {}", baseDir.toAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to initialize application directories", e);
        }
    }

    private static void copyDefaultConfig() throws IOException {
        try (InputStream in = AppInitializer.class.getResourceAsStream("/config/default-config.properties")) {
            if (in == null) {
                throw new FileNotFoundException("default-config.properties not found in resources.");
            }
            Files.copy(in, configFile);
            logger.info("Default configuration file created at: {}", configFile.toAbsolutePath());
        }
    }

    public static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
            logger.info("Configuration loaded successfully from: {}", configFile.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to load configuration file from {}", configFile.toAbsolutePath(), e);
        }
        return props;
    }
}
