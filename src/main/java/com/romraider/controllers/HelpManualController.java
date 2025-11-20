package com.romraider.controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class HelpManualController {

    @FXML
    private WebView webView;

    @FXML
    private void initialize() {
        // Cargar el HTML desde resources
        webView.getEngine().load(
                getClass().getResource("/manual/manual.html").toExternalForm()
        );
    }
}
