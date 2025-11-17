package com.romraider.app;

import com.romraider.db.DataInitializer;
import com.romraider.db.JpaUtil;
import com.romraider.utils.SceneUtils;
import com.romraider.utils.SoundUtils;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clase principal de la aplicación ROM Raider.
 *
 * Gestiona:
 *  - Pantalla de inicio (splash screen)
 *  - Inicialización de configuración y recursos
 *  - Lanzamiento de la pantalla de Login
 *  - Cierre ordenado del EntityManager (JPA)
 *
 * Extiende {@link Application} de JavaFX.
 */
public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Punto de entrada gráfico de JavaFX.
     * Configura y muestra la pantalla de splash, reproduce un sonido de arranque
     * y, tras un retardo, cambia a la vista de Login.
     *
     * @param splashStage stage inicial proporcionado por JavaFX
     */
    @Override
    public void start(Stage splashStage) {
        logger.info("Iniciando ROM Raider...");

        // Inicializa propiedades, carpetas y configuración general
        AppInitializer.initialize();

        // Si quisieras cargar datos por defecto:
        DataInitializer.initializeWithDefaults();

        // --- Pantalla de Splash con logo ---
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

        // --- Temporizador antes de pasar a la vista de login ---
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            splashStage.close();
            logger.debug("Pantalla de splash cerrada");

            Stage mainStage = new Stage();
            SceneUtils.switchToLoginView(mainStage);
        });
        delay.play();
    }

    /**
     * Se ejecuta al cerrar la aplicación.
     * Garantiza el cierre del EntityManagerFactory para evitar fugas.
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
