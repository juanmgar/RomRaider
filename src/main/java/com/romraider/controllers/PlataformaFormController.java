package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.service.PlataformaService;
import com.romraider.utils.I18nUtils;
import com.romraider.utils.MessageUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del formulario de creación o edición de plataformas.
 *
 * <p>Esta vista permite gestionar las propiedades básicas de una plataforma,
 * incluyendo:</p>
 *
 * <ul>
 *     <li><b>Nombre</b> de la plataforma (ej.: "NES", "PlayStation")</li>
 *     <li><b>Extensión de archivo</b> asociada a sus ROMs (ej.: ".nes", ".sfc")</li>
 *     <li><b>Carpeta interna</b> donde se almacenan sus ROMs dentro del sistema</li>
 * </ul>
 *
 * <p>El controlador gestiona:</p>
 * <ul>
 *     <li>Validación ligera reactiva (enable/disable del botón Guardar)</li>
 *     <li>Validación estricta en el guardado con mensajes al usuario</li>
 *     <li>Soporte para modo edición (carga previa de datos)</li>
 *     <li>Persistencia mediante {@link PlataformaService}</li>
 * </ul>
 *
 * <p>Al finalizar, la plataforma se guarda en la base local y se cierra la ventana.</p>
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

    /** Servicio encargado de la persistencia de plataformas. */
    private final PlataformaService plataformaService = new PlataformaService();

    /**
     * Plataforma cargada en modo edición.
     * <p>Si es {@code null}, el formulario funcionará en modo creación.</p>
     */
    private Plataforma plataformaToEdit;

    /**
     * Inicializa el formulario añadiendo listeners de validación en tiempo real.
     *
     * <p>Los listeners actualizan el estado del botón Guardar
     * según los valores introducidos.</p>
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
     * @param plataforma la plataforma seleccionada desde la vista principal
     */
    public void setPlataformaToEdit(Plataforma plataforma) {
        this.plataformaToEdit = plataforma;

        nameField.setText(plataforma.getNombre());
        extField.setText(plataforma.getExtensionRom());
        folderField.setText(plataforma.getCarpeta());

        logger.info("Formulario cargado para editar plataforma '{}'", plataforma.getNombre());
    }

    /**
     * Valida de forma ligera los campos del formulario.
     *
     * <p>Se encarga únicamente de activar o desactivar el botón Guardar,
     * sin mostrar mensajes al usuario.</p>
     *
     * <p>Reglas comprobadas:</p>
     * <ul>
     *     <li>El nombre no puede estar vacío.</li>
     *     <li>La extensión debe empezar por punto y contener 1 a 10 caracteres alfanuméricos.</li>
     *     <li>La carpeta no puede estar vacía.</li>
     * </ul>
     */
    private void validate() {
        boolean valid =
                !nameField.getText().trim().isEmpty()
                        && extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")
                        && !folderField.getText().trim().isEmpty();

        saveButton.setDisable(!valid);
    }

    /**
     * Acción del botón Guardar.
     *
     * <p>Realiza validación estricta y, si es correcta:</p>
     * <ul>
     *     <li>Actualiza una plataforma existente o crea una nueva instancia.</li>
     *     <li>La persiste mediante {@link PlataformaService}.</li>
     *     <li>Muestra un mensaje de éxito.</li>
     *     <li>Cierra el formulario.</li>
     * </ul>
     */
    @FXML
    public void handleSave() {
        if (!validateFields()) return;

        Plataforma plataforma = (plataformaToEdit != null) ? plataformaToEdit : new Plataforma();

        plataforma.setNombre(nameField.getText().trim());
        plataforma.setExtensionRom(extField.getText().trim());
        plataforma.setCarpeta(folderField.getText().trim());

        plataformaService.guardar(plataforma);

        logger.info("Plataforma '{}' guardada correctamente", plataforma.getNombre());
        MessageUtils.showInfo(I18nUtils.get("platformForm.saveSuccess"));
        closeWindow();
    }

    /**
     * Acción del botón Cancelar.
     *
     * <p>Cierra el formulario sin guardar ni modificar la plataforma cargada.</p>
     */
    @FXML
    public void handleCancel() {
        logger.info("El usuario ha cancelado el formulario de plataforma");
        closeWindow();
    }

    /**
     * Validación estricta de campos antes del guardado.
     *
     * <p>Si algún dato no es válido, se muestra un mensaje específico
     * mediante {@link MessageUtils}.</p>
     *
     * @return {@code true} si todos los campos son válidos, {@code false} si hay errores
     */
    private boolean validateFields() {
        if (nameField.getText().trim().isEmpty()) {
            MessageUtils.showWarning(I18nUtils.get("platformForm.errorName"));
            return false;
        }
        if (!extField.getText().trim().matches("^\\.[a-zA-Z0-9]{1,10}$")) {
            MessageUtils.showWarning(I18nUtils.get("platformForm.errorExtension"));
            return false;
        }
        if (folderField.getText().trim().isEmpty()) {
            MessageUtils.showWarning(I18nUtils.get("platformForm.errorFolder"));
            return false;
        }
        return true;
    }

    /**
     * Cierra la ventana del formulario.
     *
     * <p>Obtiene el {@link Stage} desde cualquiera de los campos
     * y ejecuta su cierre.</p>
     */
    private void closeWindow() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
        logger.info("Ventana de formulario de plataforma cerrada");
    }
}
