package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.service.PlataformaService;
import com.romraider.utils.MessageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlataformaFormController {

    private static final Logger logger = LoggerFactory.getLogger(PlataformaFormController.class);

    @FXML
    private TextField nameField;
    @FXML
    private TextField extField;
    @FXML
    private TextField folderField;
    @FXML
    private Button saveButton;

    private final PlataformaService plataformaService = new PlataformaService();
    private Plataforma plataformaToEdit;

    @FXML
    public void initialize() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        extField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        folderField.textProperty().addListener((obs, oldVal, newVal) -> validate());
    }

    public void setPlataformaToEdit(Plataforma plataforma) {
        this.plataformaToEdit = plataforma;

        nameField.setText(plataforma.getNombre());
        extField.setText(plataforma.getExtensionRom());
        folderField.setText(plataforma.getCarpeta());
    }

    private void validate() {
        boolean valid = !nameField.getText().trim().isEmpty()
                && extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")
                && !folderField.getText().trim().isEmpty();

        saveButton.setDisable(!valid);
    }

    @FXML
    public void handleSave() {
        if (!validateFields()) return;

        Plataforma plataforma = (plataformaToEdit != null) ? plataformaToEdit : new Plataforma();

        plataforma.setNombre(nameField.getText().trim());
        plataforma.setExtensionRom(extField.getText().trim());
        plataforma.setCarpeta(folderField.getText().trim());

        plataformaService.guardar(plataforma);
        MessageUtils.showInfo("Platform saved successfully.");
        closeWindow();
    }

    @FXML
    public void handleCancel() {
        logger.info("Platform form cancelled by user");
        closeWindow();
    }

    private boolean validateFields() {
        if (nameField.getText().trim().isEmpty()) {
            MessageUtils.showWarning("Name is required.");
            return false;
        }
        if (!extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")) {
            MessageUtils.showWarning("Invalid extension format. Must start with '.' and contain up to 10 alphanumeric characters.");
            return false;
        }
        if (folderField.getText().trim().isEmpty()) {
            MessageUtils.showWarning("Folder path is required.");
            return false;
        }
        return true;
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
