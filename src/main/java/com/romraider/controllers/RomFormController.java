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

/**
 * Controlador del formulario de creación/edición de ROMs.
 * Gestiona:
 * - Título
 * - Descripción
 * - Imagen de portada
 * - Flags de favorito y jugado
 * - Plataforma asociada
 *
 * Permite cargar una ROM existente (modo edición) o crear una nueva ROM.
 */
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

    /**
     * Inicializa el formulario cargando todas las plataformas en el ComboBox
     * y activando validación dinámica conforme el usuario introduce datos.
     */
    @FXML
    public void initialize() {
        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        platformComboBox.setItems(FXCollections.observableArrayList(plataformas));

        // Validación reactiva conforme el usuario escribe
        titleField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        imageField.textProperty().addListener((obs, oldVal, newVal) -> validate());
        platformComboBox.valueProperty().addListener((obs, oldVal, newVal) -> validate());
    }

    /**
     * Carga una ROM en el formulario para editarla.
     *
     * @param rom la ROM seleccionada desde la vista principal
     */
    public void setRomToEdit(Rom rom) {
        this.romToEdit = rom;

        titleField.setText(rom.getTitulo());
        descriptionArea.setText(rom.getDescripcion());
        imageField.setText(rom.getImagen());
        favoriteCheckBox.setSelected(rom.isFavorito());
        playedCheckBox.setSelected(rom.isJugado());
        platformComboBox.setValue(rom.getPlataforma());

        logger.info("Formulario de ROM cargado en modo edición para '{}'", rom.getTitulo());
    }

    /**
     * Abre un diálogo para seleccionar una imagen y la copia a la carpeta interna del programa.
     */
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
                // Nombre temporal si el usuario aún no asignó título
                String romTitle = titleField.getText().trim().isEmpty()
                        ? "untitled"
                        : titleField.getText().trim();

                // Copiar imagen usando utilidades del programa
                String copiedImagePath = ImageUtils.copyImageToLocalFolder(selectedFile, romTitle);

                imageField.setText(copiedImagePath);
                validate();

                logger.info("Imagen '{}' copiada correctamente a '{}'", selectedFile.getName(), copiedImagePath);

            } catch (IOException e) {
                logger.error("Error al copiar imagen seleccionada", e);
                MessageUtils.showError("Failed to copy image: " + e.getMessage());
            }
        }
    }

    /**
     * Guarda la ROM creada o editada después de validar los campos.
     */
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

        logger.info("ROM '{}' guardada correctamente", rom.getTitulo());
        MessageUtils.showInfo("ROM saved successfully.");
        closeWindow();
    }

    /**
     * Cierra el formulario sin realizar cambios.
     */
    @FXML
    public void handleCancel() {
        logger.info("El usuario ha cancelado el formulario de ROM");
        closeWindow();
    }

    /**
     * Validación ligera utilizada para activar o desactivar el botón de guardar.
     */
    private void validate() {
        boolean isTitleValid = titleField.getText() != null && !titleField.getText().trim().isEmpty();
        boolean isImageValid = imageField.getText() != null && !imageField.getText().trim().isEmpty();
        boolean isPlatformValid = platformComboBox.getValue() != null;

        saveButton.setDisable(!(isTitleValid && isImageValid && isPlatformValid));
    }

    /**
     * Validación estricta usada antes de guardar.
     *
     * @return true si todos los campos requeridos están correctos
     */
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

    /**
     * Cierra la ventana actual del formulario.
     */
    private void closeWindow() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
        logger.info("Ventana de formulario de ROM cerrada");
    }
}
