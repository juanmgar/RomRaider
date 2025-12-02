package com.romraider.controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

/**
 * Controlador encargado de mostrar el manual de ayuda de la aplicación.
 *
 * <p>Carga un documento HTML incrustado en los recursos del proyecto
 * dentro de un componente {@link WebView}.</p>
 *
 * <p>El archivo se espera en la ruta:
 * <pre>/manual/manual.html</pre>
 * dentro del classpath.</p>
 */
public class HelpManualController {

    /**
     * Componente WebView donde se renderiza el manual HTML.
     */
    @FXML
    private WebView webView;

    /**
     * Inicializa el controlador cargando el manual de ayuda desde
     * los recursos del proyecto en el {@link WebView}.
     *
     * <p>Este método es llamado automáticamente por JavaFX tras
     * cargar el FXML asociado.</p>
     */
    @FXML
    private void initialize() {
        // Cargar el HTML desde resources
        webView.getEngine().load(
                getClass().getResource("/manual/manual.html").toExternalForm()
        );
    }
}
