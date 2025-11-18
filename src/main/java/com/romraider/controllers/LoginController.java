package com.romraider.controllers;

import com.romraider.api.SupabaseAuthService;
import com.romraider.api.SupabaseSyncService;
import com.romraider.auth.SessionManager;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.MessageUtils;
import com.romraider.utils.NetworkUtils;
import com.romraider.utils.SceneUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador encargado del proceso de login, registro y acceso offline.
 * Gestiona tanto la autenticación con Supabase como el auto-login mediante
 * token persistido en disco.
 *
 * Este controlador es el punto de entrada de la aplicación.
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private static final PlataformaService plataformaService = new PlataformaService();
    private static final RomService romService = new RomService();

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

    /**
     * Inicializa la pantalla de login.
     * - Comprueba conexión a Internet.
     * - Restaura sesión (token) si existe.
     * - Si hay sesión válida, entra automáticamente a la vista principal.
     */
    @FXML
    public void initialize() {
        boolean online = NetworkUtils.isInternetAvailable();

        if (!online) {
            // Offline: deshabilitar login y registro, pero permitir acceso sin autenticación
            loginButton.setDisable(true);
            registerButton.setDisable(true);
            messageLabel.setText("No internet connection. You can continue offline.");
            logger.warn("No hay conexión a Internet. Modo offline habilitado.");
        }

        String token = SessionManager.loadSession();
        if (token != null) {
            logger.info("Intentando auto-login mediante sesión guardada...");

            // Restaurar sesión usando el token almacenado
            if (SupabaseAuthService.restoreSession(token)) {
                logger.info("Auto-login exitoso. Cargando pantalla principal...");
                Platform.runLater(() -> {
                    Stage stage = (Stage) usernameField.getScene().getWindow();
                    SceneUtils.switchToMainView(stage);
                });
            }
        } else {
            logger.info("No se encontró token de sesión. Mostrando formulario de login.");
        }
    }

    /**
     * Maneja el inicio de sesión usando Supabase.
     * Si el login es exitoso:
     * - Guarda la sesión si "Remember me" está marcado.
     * - Lanza una sincronización inicial con Supabase.
     * - Cambia a la vista principal de la aplicación.
     */
    @FXML
    public void handleLogin() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        logger.info("Intentando login para usuario '{}'", email);

        // Login normal, sin spinner
        boolean success = SupabaseAuthService.login(email, password);

        if (!success) {
            messageLabel.setText("Invalid username or password");
            logger.warn("Login fallido para {}", email);
            return;
        }

        logger.info("Login exitoso para {}", email);

        // Guardar sesión si procede
        if (rememberMeCheck.isSelected()) {
            SessionManager.saveSession(SupabaseAuthService.getRefreshToken());
            logger.info("Sesión guardada en disco para auto-login futuro");
        }

        // Spinner durante la sincronización
        mostrarSpinnerSincronizacionYSync();
    }


    /**
     * Maneja el registro de un nuevo usuario en Supabase.
     * Si el registro es correcto, se indica al usuario que debe confirmar su email.
     */
    @FXML
    public void handleRegister() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        logger.info("Intentando registrar usuario '{}'", email);
        boolean success = SupabaseAuthService.register(email, password);

        if (success) {
            messageLabel.setText("Account created. Please check your email to confirm before logging in.");
            logger.info("Registro exitoso. Email de confirmación enviado a {}", email);
        } else {
            messageLabel.setText("Registration failed. The email might already be in use.");
            logger.warn("Error en registro para {}", email);
        }
    }

    /**
     * Permite entrar al sistema en modo offline.
     * Limpia cualquier dato existente en Supabase local (plataformas y ROMs)
     * para garantizar coherencia con la base local independiente.
     */
    @FXML
    public void handleOffline() {
        logger.info("Accediendo en modo offline. Eliminando datos locales...");

        plataformaService.eliminarTodas();
        romService.eliminarTodas();

        Stage stage = (Stage) usernameField.getScene().getWindow();
        SceneUtils.switchToMainView(stage);

        logger.info("Modo offline activado con éxito. Datos locales reinicializados.");
    }

    private void mostrarSpinnerSincronizacionYSync() {

        // Crear mensaje
        Label mensaje = new Label("Synchronizing with the cloud...");
        mensaje.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(80, 80);

        VBox content = new VBox(15, spinner, mensaje);
        content.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(content);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6)");
        StackPane.setAlignment(spinner, Pos.CENTER);

        Pane root = (Pane) usernameField.getScene().getRoot();
        root.getChildren().add(overlay);

        loginButton.setDisable(true);
        registerButton.setDisable(true);

        // Ejecutar sincronización en segundo plano
        Task<Void> syncTask = new Task<>() {
            @Override
            protected Void call() {
                logger.info("Iniciando sincronización inicial tras login...");
                SupabaseSyncService.syncWithSupabase();
                return null;
            }
        };

        syncTask.setOnSucceeded(e -> {
            root.getChildren().remove(overlay);
            loginButton.setDisable(false);
            registerButton.setDisable(false);

            // Cambiar a pantalla principal
            Stage stage = (Stage) usernameField.getScene().getWindow();
            SceneUtils.switchToMainView(stage);
        });

        syncTask.setOnFailed(e -> {
            root.getChildren().remove(overlay);
            loginButton.setDisable(false);
            registerButton.setDisable(false);

            MessageUtils.showError(
                    "Synchronization failed: " + syncTask.getException().getMessage()
            );
            logger.error("Sincronización fallida", syncTask.getException());
        });

        new Thread(syncTask).start();
    }

}
