package com.romraider.controllers;

import com.romraider.utils.AppInitializer;
import com.romraider.utils.MessageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class PreferencesController {

    private static final Logger logger = LoggerFactory.getLogger(PreferencesController.class);

    @FXML
    private TextField copyPathField;
    @FXML
    private CheckBox autoUpdateCheckBox;

    @FXML
    public void initialize() {
        Properties config = AppInitializer.loadConfig();

        boolean autoUpdate = Boolean.parseBoolean(config.getProperty("romraider.api.autoupdate", "true"));
        autoUpdateCheckBox.setSelected(autoUpdate);

        String relativePath = config.getProperty("romraider.roms.default-folder", "ROMRaider/roms");
        String resolvedPath = Paths.get(System.getProperty("user.home"), relativePath).toString();
        copyPathField.setText(resolvedPath);

        logger.debug("Loaded preferences: autoUpdate={}, copyPath={}", autoUpdate, resolvedPath);
    }

    @FXML
    public void handleBrowseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select ROM Copy Folder");
        File selectedDir = chooser.showDialog(null);
        if (selectedDir != null) {
            copyPathField.setText(selectedDir.getAbsolutePath());
            logger.info("Selected copy path: {}", selectedDir.getAbsolutePath());
        } else {
            logger.info("No folder selected in directory chooser");
        }
    }

    @FXML
    public void handleSave() {
        String absolutePath = copyPathField.getText().trim();
        boolean autoUpdate = autoUpdateCheckBox.isSelected();

        logger.info("Saving preferences...");
        logger.debug("Copy Path: {}", absolutePath);
        logger.debug("Auto Update RAWG: {}", autoUpdate);

        String userHome = System.getProperty("user.home");
        String relativePath = absolutePath.startsWith(userHome)
                ? Paths.get(userHome).relativize(Paths.get(absolutePath)).toString()
                : absolutePath;

        Properties config = AppInitializer.loadConfig();
        config.setProperty("romraider.api.autoupdate", String.valueOf(autoUpdate));
        config.setProperty("romraider.roms.default-folder", relativePath);

        try (OutputStream out = Files.newOutputStream(AppInitializer.configFile)) {
            config.store(out, "ROM Raider Configuration");
            MessageUtils.showInfo("Preferences saved successfully.");
            logger.info("Preferences saved successfully.");
        } catch (IOException e) {
            MessageUtils.showError("Error saving preferences: " + e.getMessage());
            logger.error("Error saving preferences", e);
        }

        closeWindow();
    }

    @FXML
    public void handleCancel() {
        logger.info("Preferences dialog cancelled by user");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) copyPathField.getScene().getWindow();
        stage.close();
    }
}
