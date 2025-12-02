package com.romraider.app;

import com.romraider.utils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Inicializador principal de la aplicación.
 *
 * <p>Su responsabilidad es garantizar que el entorno local necesario para
 * ejecutar RomRaider esté correctamente preparado, creando:</p>
 *
 * <ul>
 *     <li>Directorio base: {@code ~/.romraider}</li>
 *     <li>Subcarpetas: {@code config}, {@code db}, {@code logs}</li>
 *     <li>Archivo de configuración inicial {@code romraider.properties},
 *          copiado desde los recursos si no existe.</li>
 * </ul>
 *
 * <p>También proporciona un método para cargar la configuración usando
 * {@link PropertyUtils}.</p>
 */
public class AppInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    /** Nombre interno de la aplicación usado para crear el directorio base. */
    private static final String APP_NAME = "romraider";

    /** Nombre del archivo de configuración principal de la aplicación. */
    private static final String CONFIG_FILE_NAME = "romraider.properties";

    /** Ruta al directorio base (~/.romraider). */
    private static Path baseDir;

    /** Ruta al directorio que almacena configuraciones (~/.romraider/config). */
    public static Path configDir;

    /** Ruta al directorio de base de datos (~/.romraider/db). */
    public static Path dbDir;

    /** Ruta al directorio de logs (~/.romraider/logs). */
    public static Path logDir;

    /** Ruta completa al archivo de configuración principal. */
    public static Path configFile;

    /**
     * Inicializa la estructura de directorios y el archivo de configuración.
     *
     * <p>Acciones realizadas:</p>
     * <ol>
     *     <li>Determina el directorio base en {@code ~/.romraider}</li>
     *     <li>Crea las carpetas {@code config}, {@code db}, {@code logs}</li>
     *     <li>Si el archivo {@code romraider.properties} no existe, copia uno por defecto</li>
     * </ol>
     *
     * <p>Cualquier error se registra en los logs.</p>
     */
    public static void initialize() {
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

            if (Files.notExists(configFile)) {
                copyDefaultConfig();
            }

            logger.info("App directories initialized at: {}", baseDir.toAbsolutePath());

        } catch (IOException e) {
            logger.error("Failed to initialize application directories", e);
        }
    }

    /**
     * Copia el archivo de configuración por defecto desde los recursos
     * a {@code ~/.romraider/config/romraider.properties}.
     *
     * @throws IOException si ocurre un error leyendo o escribiendo el archivo
     *
     * @throws FileNotFoundException si el recurso
     * {@code /config/default-config.properties} no está presente
     */
    private static void copyDefaultConfig() throws IOException {
        try (InputStream in = AppInitializer.class.getResourceAsStream("/config/default-config.properties")) {
            if (in == null) {
                throw new FileNotFoundException("default-config.properties not found in resources.");
            }
            Files.copy(in, configFile);
            logger.info("Default configuration file created at: {}", configFile.toAbsolutePath());
        }
    }

    /**
     * Carga el archivo de configuración principal utilizando {@link PropertyUtils}.
     *
     * <p>En caso de fallo, lanza una RuntimeException para detener la aplicación,
     * ya que ejecutar sin configuración válida no es seguro.</p>
     *
     * @return instancia de {@link PropertyUtils} con las propiedades cargadas
     */
    public static PropertyUtils loadConfig() {
        try {
            return new PropertyUtils(configFile.toString());
        } catch (IOException e) {
            logger.error("Failed to load configuration file from {}", configFile.toAbsolutePath(), e);
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

}
