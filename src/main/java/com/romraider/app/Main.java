package com.romraider.app;

import com.romraider.db.DataInitializer;
import com.romraider.db.JpaUtil;
import com.romraider.utils.I18nUtils;
import com.romraider.utils.PropertyUtils;
import com.romraider.utils.SceneUtils;
import com.romraider.utils.SoundUtils;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Clase principal de la aplicación ROM Raider.
 *
 * <p>Gestiona:</p>
 * <ul>
 *   <li>Pantalla de inicio (splash screen)</li>
 *   <li>Inicialización de configuración y recursos</li>
 *   <li>Lanzamiento de la pantalla de Login</li>
 *   <li>Cierre ordenado del EntityManager (JPA)</li>
 * </ul>
 *
 * <p>Extiende {@link Application} de JavaFX.</p>
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Punto de entrada gráfico de JavaFX.
     * <p>
     * Configura y muestra la pantalla de splash, reproduce un sonido de arranque
     * y, tras un retardo, cambia a la vista de Login.
     * </p>
     *
     * @param splashStage stage inicial proporcionado por JavaFX
     */
    @Override
    public void start(Stage splashStage) {
        logger.info("Iniciando ROM Raider...");

        // Inicializa propiedades, carpetas y configuración general
        AppInitializer.initialize();

        PropertyUtils config = AppInitializer.loadConfig();
        String lang = config.getOrDefault("romraider.language", "es");

        I18nUtils.load(lang);
        logger.info("Idioma de la aplicación: {}", lang);

        // Cargar datos por defecto:
        DataInitializer.initializeWithDefaults();

        // Pantalla de Splash con logo
        ImageView imageView = new ImageView(
                new Image(getClass().getResource("/assets/romraider-logo.png").toExternalForm())
        );
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(500);

        // Sonido de arranque (STARTUP)
        SoundUtils.play(SoundUtils.STARTUP);

        StackPane root = new StackPane(imageView);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(null);

        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        logger.debug("Pantalla de splash mostrada");

        // Temporizador antes de pasar a la vista de login
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            splashStage.close();
            logger.debug("Pantalla de splash cerrada");

            Stage mainStage = new Stage();
            SceneUtils.switchToLoginView(mainStage);

            mainStage.setOnCloseRequest(event -> {
                if (!confirmClose()) {
                    event.consume();
                }
            });
        });
        delay.play();
    }

    /**
     * Muestra un cuadro de diálogo de confirmación de salida.
     *
     * <p>El diálogo pregunta al usuario si realmente desea cerrar la aplicación.
     * En caso de cancelar, el cierre de la ventana principal se anula.</p>
     *
     * @return {@code true} si el usuario confirma la salida,
     *         {@code false} si cancela o cierra el diálogo.
     */
    private boolean confirmClose() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.get("app.exit.title"));
        alert.setHeaderText(I18nUtils.get("app.exit.header"));
        alert.setContentText(I18nUtils.get("app.exit.content"));

        ButtonType guardarBtn = new ButtonType(I18nUtils.get("app.exit.confirm"));
        ButtonType cancelarBtn = new ButtonType(I18nUtils.get("app.exit.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(guardarBtn, cancelarBtn);

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == guardarBtn;
    }

    /**
     * Se ejecuta al cerrar la aplicación.
     *
     * <p>Garantiza el cierre del {@link jakarta.persistence.EntityManagerFactory}
     * gestionado por {@link JpaUtil} para evitar fugas de recursos.</p>
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        JpaUtil.close();
        logger.info("EntityManagerFactory cerrado correctamente. Aplicación finalizada.");
    }

    /**
     * Método main estándar: lanza la aplicación JavaFX.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        launch(args);
    }
}
