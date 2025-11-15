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

/**
 * Controlador del formulario de creación/edición de plataformas.
 * Permite introducir datos básicos sobre una plataforma:
 * - Nombre
 * - Extensión de ROM soportada (p.ej.: ".nes")
 * - Carpeta donde se almacenan las ROMs
 *
 * También gestiona validación de campos en tiempo real y guarda la plataforma
 * en la base de datos a través de PlataformaService.
 */
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

    /** Plataforma cargada para edición. Si es null, se asume creación. */
    private Plataforma plataformaToEdit;

    /**
     * Inicializa el formulario añadiendo listeners para validar los campos
     * en tiempo real conforme el usuario escribe.
     */
    @FXML
    public void initialize() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        extField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        folderField.textProperty().addListener((obs, oldVal, newVal) -> validate());
    }

    /**
     * Carga una plataforma existente en el formulario para su edición.
     *
     * @param plataforma plataforma seleccionada para editar
     */
    public void setPlataformaToEdit(Plataforma plataforma) {
        this.plataformaToEdit = plataforma;

        nameField.setText(plataforma.getNombre());
        extField.setText(plataforma.getExtensionRom());
        folderField.setText(plataforma.getCarpeta());

        logger.info("Formulario cargado para editar plataforma '{}'", plataforma.getNombre());
    }

    /**
     * Valida los campos principales del formulario.
     * Esta validación es ligera y rápida, usada para controlar
     * el estado (enable/disable) del botón de guardar.
     *
     * La validación estricta se hace en validateFields().
     */
    private void validate() {
        boolean valid =
                !nameField.getText().trim().isEmpty()
                        && extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")
                        && !folderField.getText().trim().isEmpty();

        saveButton.setDisable(!valid);
    }

    /**
     * Botón de guardado.
     * Valida los campos, crea o actualiza la plataforma y la guarda
     * en la base de datos.
     */
    @FXML
    public void handleSave() {
        if (!validateFields()) return;

        // Crear o reutilizar la plataforma existente
        Plataforma plataforma = (plataformaToEdit != null) ? plataformaToEdit : new Plataforma();

        plataforma.setNombre(nameField.getText().trim());
        plataforma.setExtensionRom(extField.getText().trim());
        plataforma.setCarpeta(folderField.getText().trim());

        plataformaService.guardar(plataforma);

        logger.info("Plataforma '{}' guardada correctamente", plataforma.getNombre());
        MessageUtils.showInfo("Platform saved successfully.");
        closeWindow();
    }

    /**
     * Botón de cancelar.
     * Cierra el formulario sin guardar cambios.
     */
    @FXML
    public void handleCancel() {
        logger.info("El usuario ha cancelado el formulario de plataforma");
        closeWindow();
    }

    /**
     * Validación estricta de campos.
     * Muestra mensajes al usuario cuando hay errores.
     *
     * @return true si los campos son válidos, false si hay errores
     */
    private boolean validateFields() {
        if (nameField.getText().trim().isEmpty()) {
            MessageUtils.showWarning("Name is required.");
            return false;
        }
        if (!extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")) {
            MessageUtils.showWarning(
                    "Invalid extension format. Must start with '.' and contain up to 10 alphanumeric characters."
            );
            return false;
        }
        if (folderField.getText().trim().isEmpty()) {
            MessageUtils.showWarning("Folder path is required.");
            return false;
        }
        return true;
    }

    /**
     * Cierra la ventana actual del formulario.
     */
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
        logger.info("Ventana de formulario de plataforma cerrada");
    }
}
