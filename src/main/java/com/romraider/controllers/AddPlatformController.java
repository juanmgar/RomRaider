package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.service.PlataformaService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddPlatformController {

    private static final Logger logger = LoggerFactory.getLogger(AddPlatformController.class);

    @FXML
    private TextField nameField;
    @FXML
    private TextField extField;
    @FXML
    private TextField folderField;
    @FXML
    private Button saveButton;

    private final PlataformaService plataformaService = new PlataformaService();

    @FXML
    public void initialize() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        extField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        folderField.textProperty().addListener((obs, oldVal, newVal) -> validate());
    }

    private void validate() {
        boolean valid = !nameField.getText().trim().isEmpty()
                && extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")
                && !folderField.getText().trim().isEmpty();

        saveButton.setDisable(!valid);
    }

    @FXML
    public void handleSave() {
        Plataforma plataforma = new Plataforma();
        plataforma.setNombre(nameField.getText().trim());
        plataforma.setExtensionRom(extField.getText().trim());
        plataforma.setCarpeta(folderField.getText().trim());

        plataformaService.guardar(plataforma);

        closeWindow();
    }

    @FXML
    public void handleCancel() {
        logger.info("Preferences dialog cancelled by user");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
