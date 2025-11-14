package com.romraider.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class CreditsController {

    @FXML
    public void handleCloseCredits(javafx.event.ActionEvent event) {
        ((Stage) ((Button) event.getSource()).getScene().getWindow()).close();
    }

    @FXML
    private void handleOpenFreepik() {
        try {
            java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("https://www.freepik.com/free-vector/dark-background-with-geometric-design_853799.html")
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
