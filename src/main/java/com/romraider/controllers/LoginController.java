package com.romraider.controllers;

import com.romraider.utils.SceneUtils;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    private boolean offlineMode = false;

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberMeCheck;
    @FXML
    private Label messageLabel;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.equals("juanma") && password.equals("1234")) {
            System.out.println("Login successful: " + username);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            SceneUtils.switchToMainView(stage, false); // Modo online
        } else {
            messageLabel.setText("Invalid username or password");
            System.out.println("Login failed for user: " + username);
        }
    }

    @FXML
    public void handleRegister() {
        System.out.println("Register clicked");
        // TODO: implement registration
    }

    @FXML
    public void handleOffline() {
        offlineMode = true;
        System.out.println("Continuing in offline mode");

        Stage stage = (Stage) usernameField.getScene().getWindow();
        SceneUtils.switchToMainView(stage, offlineMode);
    }

}