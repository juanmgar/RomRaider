package com.romraider.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Utilidad para mostrar diálogos emergentes (alertas, avisos, confirmaciones)
 * en la interfaz JavaFX.
 *
 * Todas las ventanas que se muestran al usuario son en inglés.
 * Los mensajes de log generados desde esta clase son en español.
 */
public class MessageUtils {

    private static final Logger logger = LoggerFactory.getLogger(MessageUtils.class);

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param message texto a mostrar en el diálogo
     */
    public static void showError(String message) {
        logger.error("Mostrando mensaje de error al usuario: {}", message);
        showAlert(Alert.AlertType.ERROR, "Error", message);
    }

    /**
     * Muestra un mensaje informativo al usuario.
     *
     * @param message texto de información
     */
    public static void showInfo(String message) {
        logger.info("Mostrando mensaje informativo al usuario: {}", message);
        showAlert(Alert.AlertType.INFORMATION, "Information", message);
    }

    /**
     * Muestra un aviso al usuario.
     *
     * @param message texto del aviso
     */
    public static void showWarning(String message) {
        logger.warn("Mostrando mensaje de advertencia al usuario: {}", message);
        showAlert(Alert.AlertType.WARNING, "Warning", message);
    }

    /**
     * Muestra un diálogo de confirmación con botones OK / Cancel.
     *
     * @param message mensaje a mostrar
     * @return true si el usuario acepta (OK), false en caso contrario
     */
    public static boolean showConfirmation(String message) {
        logger.debug("Mostrando cuadro de confirmación al usuario: {}", message);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Método interno para crear y mostrar una alerta JavaFX.
     *
     * @param type tipo de alerta (error, info, warning...)
     * @param title título de la ventana (en inglés)
     * @param message contenido del mensaje
     */
    private static void showAlert(Alert.AlertType type, String title, String message) {
        /*
         * Centraliza la creación de Alert, asegurando consistencia
         * en cómo se muestran todas las ventanas emergentes.
         */
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
