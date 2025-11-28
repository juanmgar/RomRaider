package com.romraider.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utilidad para gestionar cambios de escenas (pantallas) en la aplicación.
 * Se encarga de:
 *   - Cargar los FXML
 *   - Aplicar estilos CSS
 *   - Configurar título, icono y tamaño
 */
public class SceneUtils {

    private static final Logger logger = LoggerFactory.getLogger(SceneUtils.class);

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 750;
    private static final String ICON_PATH = "/assets/romraider-icon.png";
    private static final String STYLESHEET = "/styles/romraider.css";

    /**
     * Cambia la escena actual hacia la vista principal (MainView).
     *
     * @param stage ventana principal de la aplicación
     */
    public static void switchToMainView(Stage stage) {
        try {
            logger.info("Cargando MainView.fxml");

            FXMLLoader loader = new FXMLLoader(
                    SceneUtils.class.getResource("/views/MainView.fxml"),
                    I18nUtils.getBundle()
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets()
                    .add(SceneUtils.class.getResource(STYLESHEET).toExternalForm());

            stage.setTitle(I18nUtils.get("app.title"));
            stage.setResizable(false);
            stage.getIcons().clear();
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(ICON_PATH)));
            stage.setScene(scene);
            stage.show();

            logger.debug("Vista principal cargada correctamente");

        } catch (IOException e) {
            logger.error("Error al cargar MainView.fxml", e);
        }
    }

    /**
     * Cambia la escena actual hacia la vista de login (LoginView).
     *
     * @param stage ventana principal de la aplicación
     */
    public static void switchToLoginView(Stage stage) {
        try {
            logger.info("Probando Cargando LoginView.fxml");

            FXMLLoader loader = new FXMLLoader(
                    SceneUtils.class.getResource("/views/LoginView.fxml"),
                    I18nUtils.getBundle()
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets()
                    .add(SceneUtils.class.getResource(STYLESHEET).toExternalForm());

            stage.setTitle(I18nUtils.get("app.title"));
            stage.setResizable(false);
            stage.getIcons().clear();
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(ICON_PATH)));
            stage.setScene(scene);
            stage.show();

            logger.debug("Vista de login cargada correctamente");

        } catch (IOException e) {
            logger.error("Error al cargar LoginView.fxml", e);
        }
    }
}
