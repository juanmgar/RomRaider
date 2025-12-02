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
 * <p>
 * Esta clase centraliza la carga de vistas FXML y la configuración de:
 * <ul>
 *   <li>Estilos CSS</li>
 *   <li>Icono de la aplicación</li>
 *   <li>Tamaño y título de la ventana</li>
 * </ul>
 * Ofrece métodos específicos para cambiar entre la vista principal
 * ({@code MainView}) y la vista de login ({@code LoginView}).
 */
public class SceneUtils {

    /**
     * Logger usado para registrar las operaciones de carga de escenas
     * y cualquier posible error durante la inicialización.
     */
    private static final Logger logger = LoggerFactory.getLogger(SceneUtils.class);

    /**
     * Anchura fija usada para las ventanas de la aplicación.
     */
    private static final int WIDTH = 1100;

    /**
     * Altura fija usada para las ventanas de la aplicación.
     */
    private static final int HEIGHT = 700;

    /**
     * Ruta al icono de la aplicación RomRaider.
     */
    private static final String ICON_PATH = "/assets/romraider-icon.png";

    /**
     * Ruta a la hoja de estilos principal aplicada a todas las vistas.
     */
    private static final String STYLESHEET = "/styles/romraider.css";

    /**
     * Cambia la escena actual hacia la vista principal ({@code MainView.fxml}).
     * <p>
     * El método:
     * <ul>
     *     <li>Carga el FXML correspondiente</li>
     *     <li>Aplica los estilos CSS</li>
     *     <li>Configura título, icono y tamaño</li>
     *     <li>Muestra la ventana</li>
     * </ul>
     *
     * @param stage ventana principal de la aplicación que debe actualizarse.
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
     * Cambia la escena actual hacia la vista de login ({@code LoginView.fxml}).
     * <p>
     * El método realiza exactamente las mismas configuraciones que {@link #switchToMainView(Stage)},
     * pero cargando la vista de login.
     *
     * @param stage ventana principal de la aplicación.
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
