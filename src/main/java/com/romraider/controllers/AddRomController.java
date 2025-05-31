package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.RomService;
import com.romraider.utils.MessageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AddRomController {

    private static final Logger logger = LoggerFactory.getLogger(AddRomController.class);

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private TextField imageField;
    @FXML
    private CheckBox favoriteCheckBox;
    @FXML
    private CheckBox playedCheckBox;
    @FXML
    private Button saveButton;

    private final RomService romService = new RomService();
    private Plataforma plataforma;

    @FXML
    public void initialize() {
        titleField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        imageField.textProperty().addListener((obs, oldVal, newVal) -> validate());
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select ROM Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(imageField.getScene().getWindow());

        if (selectedFile != null) {
            try {
                String romTitle = titleField.getText().trim().isEmpty()
                        ? "untitled"
                        : titleField.getText().trim();

                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.') + 1);
                String safeTitle = romTitle.replaceAll("[^a-zA-Z0-9]", "_");
                String filename = safeTitle + "_" + System.currentTimeMillis() + "." + extension;

                File destDir = new File(System.getProperty("user.home"), ".romraider/images");
                if (!destDir.exists()) destDir.mkdirs();

                File destFile = new File(destDir, filename);
                Files.copy(selectedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                imageField.setText(destFile.getAbsolutePath());
                validate();

            } catch (IOException e) {
                MessageUtils.showError("Failed to copy image: " + e.getMessage());
            }
        }
    }


    @FXML
    private void handleSave() {
        Rom rom = new Rom();
        rom.setTitulo(titleField.getText().trim());
        rom.setDescripcion(descriptionField.getText().trim());
        rom.setImagen(imageField.getText().trim());
        rom.setFavorito(favoriteCheckBox.isSelected());
        rom.setJugado(playedCheckBox.isSelected());
        rom.setPlataforma(plataforma);

        romService.guardar(rom);

        closeWindow();
    }

    private void validate() {
        boolean isTitleValid = titleField.getText() != null && !titleField.getText().trim().isEmpty();
        boolean isImagePathValid = imageField.getText() != null && !imageField.getText().trim().isEmpty();

        saveButton.setDisable(!(isTitleValid && isImagePathValid));
    }

    @FXML
    public void handleCancel() {
        logger.info("Preferences dialog cancelled by user");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }
}
