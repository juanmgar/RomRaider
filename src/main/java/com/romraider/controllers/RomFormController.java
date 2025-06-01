package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.RomService;
import com.romraider.utils.ImageUtils;
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

    private final RomService romService = new RomService();
    private Plataforma plataforma;
    private Rom romToEdit;

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    public void setRomToEdit(Rom rom) {
        this.romToEdit = rom;

        titleField.setText(rom.getTitulo());
        descriptionArea.setText(rom.getDescripcion());
        imageField.setText(rom.getImagen());
        favoriteCheckBox.setSelected(rom.isFavorito());
        playedCheckBox.setSelected(rom.isJugado());
    }

    @FXML
    public void initialize() {
        titleField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        imageField.textProperty().addListener((obs, oldVal, newVal) -> validate());
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
        if (romToEdit == null) rom.setPlataforma(plataforma);

        rom.setTitulo(titleField.getText().trim());
        rom.setDescripcion(descriptionArea.getText().trim());
        rom.setImagen(imageField.getText().trim());
        rom.setFavorito(favoriteCheckBox.isSelected());
        rom.setJugado(playedCheckBox.isSelected());

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
        saveButton.setDisable(!(isTitleValid && isImageValid));
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
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }
}
