package com.romraider.utils;

import com.romraider.controllers.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SceneUtils {

    private static final Logger logger = LoggerFactory.getLogger(SceneUtils.class);

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 750;
    private static final String TITLE = "ROM Raider";
    private static final String ICON_PATH = "/assets/romraider-icon.png";
    private static final String STYLESHEET = "/styles/romraider.css";

    public static void switchToMainView(Stage stage, boolean offlineMode) {
        try {
            logger.info("Loading MainView.fxml (offlineMode={})", offlineMode);

            FXMLLoader loader = new FXMLLoader(SceneUtils.class.getResource("/views/MainView.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setOfflineMode(offlineMode);

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add(SceneUtils.class.getResource(STYLESHEET).toExternalForm());

            stage.setTitle(TITLE);
            stage.getIcons().clear();
            stage.setResizable(false);
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(ICON_PATH)));
            stage.setScene(scene);
            stage.show();

            logger.debug("Main view loaded and shown");

        } catch (IOException e) {
            logger.error("Failed to load MainView.fxml", e);
        }
    }

    public static void switchToLoginView(Stage stage) {
        try {
            logger.info("Loading LoginView.fxml");

            FXMLLoader loader = new FXMLLoader(SceneUtils.class.getResource("/views/LoginView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, WIDTH, HEIGHT);
            scene.getStylesheets().add(SceneUtils.class.getResource(STYLESHEET).toExternalForm());

            stage.setTitle(TITLE);
            stage.getIcons().clear();
            stage.setResizable(false);
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(ICON_PATH)));
            stage.setScene(scene);
            stage.show();

            logger.debug("Login view loaded and shown");

        } catch (IOException e) {
            logger.error("Failed to load LoginView.fxml", e);
        }
    }
}
