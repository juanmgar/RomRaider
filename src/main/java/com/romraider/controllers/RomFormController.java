package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.ImageUtils;
import com.romraider.utils.MessageUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RomFormController {

    private static final Logger logger = LoggerFactory.getLogger(RomFormController.class);

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField imageField;
    @FXML
    private CheckBox favoriteCheckBox;
    @FXML
    private CheckBox playedCheckBox;
    @FXML
    private Button saveButton;
    @FXML
    private ComboBox<Plataforma> platformComboBox;

    private final RomService romService = new RomService();
    private final PlataformaService plataformaService = new PlataformaService();
    private Rom romToEdit;

    @FXML
    public void initialize() {
        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        platformComboBox.setItems(FXCollections.observableArrayList(plataformas));

        titleField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        imageField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        platformComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validate());
    }

    public void setRomToEdit(Rom rom) {
        this.romToEdit = rom;

        titleField.setText(rom.getTitulo());
        descriptionArea.setText(rom.getDescripcion());
        imageField.setText(rom.getImagen());
        favoriteCheckBox.setSelected(rom.isFavorito());
        playedCheckBox.setSelected(rom.isJugado());
        platformComboBox.setValue(rom.getPlataforma());
    }

    @FXML
    public void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select ROM Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(titleField.getScene().getWindow());

        if (selectedFile != null) {
            try {
                String romTitle = titleField.getText().trim().isEmpty() ? "untitled" : titleField.getText().trim();
                String copiedImagePath = ImageUtils.copyImageToLocalFolder(selectedFile, romTitle);
                imageField.setText(copiedImagePath);
                validate();

            } catch (IOException e) {
                logger.error("Failed to copy image", e);
                MessageUtils.showError("Failed to copy image: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleSave() {
        if (!validateFields()) return;

        Rom rom = (romToEdit != null) ? romToEdit : new Rom();

        Plataforma selectedPlatform = platformComboBox.getValue();
        if (selectedPlatform == null) {
            MessageUtils.showWarning("Platform is required.");
            return;
        }

        rom.setTitulo(titleField.getText().trim());
        rom.setDescripcion(descriptionArea.getText().trim());
        rom.setImagen(imageField.getText().trim());
        rom.setFavorito(favoriteCheckBox.isSelected());
        rom.setJugado(playedCheckBox.isSelected());
        rom.setPlataforma(selectedPlatform);

        romService.guardar(rom);
        MessageUtils.showInfo("ROM saved successfully.");
        closeWindow();
    }

    @FXML
    public void handleCancel() {
        logger.info("ROM form cancelled by user.");
        closeWindow();
    }

    private void validate() {
        boolean isTitleValid = titleField.getText() != null && !titleField.getText().trim().isEmpty();
        boolean isImageValid = imageField.getText() != null && !imageField.getText().trim().isEmpty();
        boolean isPlatformValid = platformComboBox.getValue() != null;
        saveButton.setDisable(!(isTitleValid && isImageValid && isPlatformValid));
    }

    private boolean validateFields() {
        if (titleField.getText().trim().isEmpty()) {
            MessageUtils.showWarning("Title is required.");
            return false;
        }
        if (imageField.getText().trim().isEmpty()) {
            MessageUtils.showWarning("Image is required.");
            return false;
        }
        if (platformComboBox.getValue() == null) {
            MessageUtils.showWarning("Platform is required.");
            return false;
        }
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }
}