package com.romraider.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Controlador de la ventana de créditos.
 * Esta vista muestra información sobre los autores y recursos utilizados.
 *
 * <p>Incluye:</p>
 * <ul>
 *     <li>Botón para cerrar la ventana.</li>
 *     <li>Enlaces directos a las páginas de Freepik y Flaticon utilizadas como recursos gráficos.</li>
 * </ul>
 */
public class CreditsController {

    private static final Logger logger = LoggerFactory.getLogger(CreditsController.class);

    /**
     * Cierra la ventana de créditos.
     *
     * @param event evento generado por el botón Close
     */
    @FXML
    public void handleCloseCredits(javafx.event.ActionEvent event) {
        logger.info("Cerrando ventana de créditos");
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }

    /**
     * Abre en el navegador predeterminado la página de Freepik
     * utilizada como recurso gráfico en la aplicación.
     */
    @FXML
    private void handleOpenFreepik() {
        openLink("https://www.freepik.com/free-vector/dark-background-with-geometric-design_853799.html",
                "Freepik");
    }

    /**
     * Abre en el navegador predeterminado la página de Flaticon
     * asociada al autor de los iconos utilizados en la aplicación.
     */
    @FXML
    private void handleOpenFlaticon() {
        openLink("https://www.flaticon.es/autores/freepik",
                "Flaticon");
    }

    /**
     * Abre un enlace externo en el navegador del sistema.
     *
     * @param url        URL a abrir
     * @param sourceName nombre descriptivo del origen del recurso (para logs)
     */
    private void openLink(String url, String sourceName) {
        try {
            logger.info("Abriendo enlace de {}", sourceName);
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            logger.error("No se pudo abrir el enlace de {}", sourceName, e);
        }
    }
}
