package com.romraider.controllers;

import com.romraider.utils.AppInitializer;
import com.romraider.utils.MessageUtils;
import com.romraider.utils.PropertyUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Controlador de la ventana de preferencias del usuario.
 *
 * Permite configurar:
 *  - Carpeta donde se copiarán automáticamente las ROMs escaneadas.
 *  - Activación o desactivación de la actualización automática desde RAWG.io.
 *
 * Estos valores se almacenan en el fichero de configuración manejado
 * por {@link PropertyUtils}.
 */
public class PreferencesController {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesController.class);

    @FXML
    private TextField copyPathField;

    @FXML
    private CheckBox autoUpdateCheckBox;

    /**
     * Inicializa la ventana de preferencias cargando los valores actuales
     * desde el archivo de configuración.
     */
    @FXML
    public void initialize() {

        PropertyUtils config = AppInitializer.loadConfig();

        // Carga del valor booleano para actualización automática desde RAWG.io
        boolean autoUpdate = Boolean.parseBoolean(config.getOrDefault("romraider.api.autoupdate", "true"));
        autoUpdateCheckBox.setSelected(autoUpdate);

        // Carga de la ruta de copia relativa configurada y conversión a absoluta
        String relativePath = config.getOrDefault("romraider.roms.default-folder", "ROMRaider/roms");
        String resolvedPath = Paths.get(System.getProperty("user.home"), relativePath).toString();
        copyPathField.setText(resolvedPath);

        logger.debug("Preferencias cargadas: autoUpdate={}, copyPath={}", autoUpdate, resolvedPath);
    }

    /**
     * Permite al usuario seleccionar una carpeta del sistema para copiar ROMs.
     * Se abre un diálogo nativo de selección de directorios.
     */
    @FXML
    public void handleBrowseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select ROM Copy Folder");

        File selectedDir = chooser.showDialog(null);

        if (selectedDir != null) {
            copyPathField.setText(selectedDir.getAbsolutePath());
            logger.info("Carpeta seleccionada por el usuario: {}", selectedDir.getAbsolutePath());
        } else {
            logger.info("El usuario no seleccionó ninguna carpeta");
        }
    }

    /**
     * Guarda las preferencias del usuario en el archivo de configuración:
     * - Ruta de copia relativa respecto al home del usuario
     * - Configuración de actualización automática
     *
     * Si ocurre un error, se muestra un mensaje al usuario en inglés.
     */
    @FXML
    public void handleSave() {

        String absolutePath = copyPathField.getText().trim();
        boolean autoUpdate = autoUpdateCheckBox.isSelected();

        logger.info("Guardando preferencias...");
        logger.debug("Ruta absoluta indicada: {}", absolutePath);
        logger.debug("RAWG auto-update activado: {}", autoUpdate);

        // Convertir ruta absoluta en relativa al home del usuario, si aplica
        String userHome = System.getProperty("user.home");

        String relativePath = absolutePath.startsWith(userHome)
                ? Paths.get(userHome).relativize(Paths.get(absolutePath)).toString()
                : absolutePath;

        try {
            PropertyUtils config = AppInitializer.loadConfig();

            config.set("romraider.api.autoupdate", String.valueOf(autoUpdate));
            config.set("romraider.roms.default-folder", relativePath);

            config.save("ROM Raider Configuration");

            logger.info("Preferencias guardadas con éxito");
            MessageUtils.showInfo("Preferences saved successfully.");

        } catch (IOException e) {
            logger.error("Error guardando preferencias", e);
            MessageUtils.showError("Error saving preferences: " + e.getMessage());
        }

        closeWindow();
    }

    /**
     * Acción del botón Cancel.
     * Cierra la ventana sin guardar cambios.
     */
    @FXML
    public void handleCancel() {
        logger.info("El usuario canceló los cambios de preferencias");
        closeWindow();
    }

    /**
     * Cierra la ventana de preferencias.
     */
    private void closeWindow() {
        Stage stage = (Stage) copyPathField.getScene().getWindow();
        stage.close();
    }
}
