package com.romraider.controllers;

import com.romraider.api.SupabaseAuthService;
import com.romraider.api.SupabaseSyncService;
import com.romraider.auth.SessionManager;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.I18nUtils;
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
 * Controlador responsable del proceso completo de autenticación de la aplicación.
 *
 * <p>Gestiona:</p>
 * <ul>
 *     <li>Inicio de sesión mediante Supabase.</li>
 *     <li>Registro de nuevos usuarios.</li>
 *     <li>Acceso en modo offline.</li>
 *     <li>Restauración automática de sesión usando un refresh token persistido.</li>
 *     <li>Sincronización inicial de datos tras un login exitoso.</li>
 * </ul>
 *
 * <p>Esta clase actúa como punto de entrada de la aplicación, siendo la primera
 * pantalla interactiva que ve el usuario.</p>
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
     * Inicializa la pantalla de login realizando diversas comprobaciones previas:
     *
     * <ul>
     *     <li>Comprueba disponibilidad de Internet para habilitar/deshabilitar funcionalidades online.</li>
     *     <li>Intenta restaurar una sesión previa mediante refresh token persistido.</li>
     *     <li>Si la restauración es exitosa, se redirige inmediatamente a la vista principal.</li>
     * </ul>
     */
    @FXML
    public void initialize() {
        boolean online = NetworkUtils.isInternetAvailable();

        if (!online) {
            loginButton.setDisable(true);
            registerButton.setDisable(true);
            messageLabel.setText(I18nUtils.get("login.offlineMode"));
            logger.warn("No hay conexión a Internet. Modo offline habilitado.");
        }

        String token = SessionManager.loadSession();
        if (token != null) {
            logger.info("Intentando auto-login mediante sesión guardada...");

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
     * Maneja el proceso completo de inicio de sesión mediante Supabase.
     *
     * <p>Flujo:</p>
     * <ol>
     *     <li>Valida credenciales con {@link SupabaseAuthService#login(String, String)}.</li>
     *     <li>Si el login es exitoso y “Remember me” está activo, se persiste el refresh token.</li>
     *     <li>Se muestra un overlay con spinner mientras se ejecuta la sincronización inicial.</li>
     *     <li>Tras sincronizar, se navega a la vista principal.</li>
     * </ol>
     */
    @FXML
    public void handleLogin() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        logger.info("Intentando login para usuario '{}'", email);

        boolean success = SupabaseAuthService.login(email, password);

        if (!success) {
            messageLabel.setText(I18nUtils.get("login.invalidCredentials"));
            logger.warn("Login fallido para {}", email);
            return;
        }

        logger.info("Login exitoso para {}", email);

        if (rememberMeCheck.isSelected()) {
            SessionManager.saveSession(SupabaseAuthService.getRefreshToken());
            logger.info("Sesión guardada en disco para auto-login futuro");
        }

        mostrarSpinnerSincronizacionYSync();
    }

    /**
     * Maneja el registro de un nuevo usuario.
     *
     * <p>Si el registro es exitoso:</p>
     * <ul>
     *     <li>Se notifica al usuario que debe confirmar su correo electrónico.</li>
     * </ul>
     *
     * <p>En caso de error, se muestra un mensaje adecuado en pantalla.</p>
     */
    @FXML
    public void handleRegister() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        logger.info("Intentando registrar usuario '{}'", email);
        boolean success = SupabaseAuthService.register(email, password);

        if (success) {
            messageLabel.setText(I18nUtils.get("login.registerSuccess"));
            logger.info("Registro exitoso. Email de confirmación enviado a {}", email);
        } else {
            messageLabel.setText(I18nUtils.get("login.registerError"));
            logger.warn("Error en registro para {}", email);
        }
    }

    /**
     * Permite acceder a la aplicación sin conexión y sin autenticación.
     *
     * <p>Acciones realizadas:</p>
     * <ul>
     *     <li>Elimina todas las plataformas y ROMs locales para evitar incoherencias.</li>
     *     <li>Abre la vista principal en modo completamente local.</li>
     * </ul>
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

    /**
     * Muestra una superposición con un spinner mientras se ejecuta la sincronización inicial
     * con Supabase en un hilo secundario.
     *
     * <p>Comportamiento:</p>
     * <ul>
     *     <li>Bloquea botones de login/registro.</li>
     *     <li>Ejecuta {@link SupabaseSyncService#syncWithSupabase()} en background.</li>
     *     <li>Al finalizar, habilita botones nuevamente y entra en la vista principal.</li>
     *     <li>En caso de error, muestra mensaje al usuario.</li>
     * </ul>
     */
    private void mostrarSpinnerSincronizacionYSync() {

        Label mensaje = new Label(I18nUtils.get("login.syncing"));
        mensaje.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(80, 80);

        VBox content = new VBox(15, spinner, mensaje);
        content.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(content);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6)");

        Pane root = (Pane) usernameField.getScene().getRoot();
        root.getChildren().add(overlay);

        loginButton.setDisable(true);
        registerButton.setDisable(true);

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

            Stage stage = (Stage) usernameField.getScene().getWindow();
            SceneUtils.switchToMainView(stage);
        });

        syncTask.setOnFailed(e -> {
            root.getChildren().remove(overlay);
            loginButton.setDisable(false);
            registerButton.setDisable(false);

            MessageUtils.showError(I18nUtils.get("login.syncFailed") + ": " + syncTask.getException().getMessage());
            logger.error("Sincronización fallida", syncTask.getException());
        });

        new Thread(syncTask).start();
    }
}
