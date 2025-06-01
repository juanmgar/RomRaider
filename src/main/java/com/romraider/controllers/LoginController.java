package com.romraider.controllers;

import com.romraider.api.SupabaseAuthService;
import com.romraider.auth.SessionManager;
import com.romraider.utils.NetworkUtils;
import com.romraider.utils.SceneUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

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
    private Button loginButton;
    @FXML
    private Button registerButton;

    @FXML
    public void initialize() {
        boolean online = NetworkUtils.isInternetAvailable();

        if (!online) {
            loginButton.setDisable(true);
            registerButton.setDisable(true);
            messageLabel.setText("No internet connection. You can continue offline.");
        }

        String token = SessionManager.loadSession();
        if (token != null) {
            logger.info("Auto-login with saved session");
            Platform.runLater(() -> {
                Stage stage = (Stage) usernameField.getScene().getWindow();
                SceneUtils.switchToMainView(stage, false);
            });
        } else {
            logger.info("No token found. Showing login form.");
        }
    }

    @FXML
    public void handleLogin() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        boolean success = SupabaseAuthService.login(email, password);

        if (success) {
            logger.info("Login successful for user: {}", email);

            if (rememberMeCheck.isSelected()) {
                SessionManager.saveSession(SupabaseAuthService.getAccessToken());
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            SceneUtils.switchToMainView(stage, false);
        } else {
            messageLabel.setText("Invalid username or password");
            logger.warn("Login failed for user: {}", email);
        }
    }

    @FXML
    public void handleRegister() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        boolean success = SupabaseAuthService.register(email, password);
        if (success) {
            messageLabel.setText("Account created. Please check your email to confirm before logging in.");
            logger.info("Registration successful. Confirmation email sent to {}", email);
        } else {
            messageLabel.setText("Registration failed. The email might already be in use.");
            logger.warn("Registration failed for {}", email);
        }
    }

    @FXML
    public void handleOffline() {
        offlineMode = true;
        logger.info("Continuing in offline mode");
        Stage stage = (Stage) usernameField.getScene().getWindow();
        SceneUtils.switchToMainView(stage, offlineMode);
    }
}