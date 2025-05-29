package com.romraider.utils;

import com.romraider.controllers.MainController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneUtils {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 700;
    private static final String TITLE = "ROM Raider";
    private static final String ICON_PATH = "/assets/romraider-icon.png";
    private static final String STYLESHEET = "/styles/romraider.css";


    public static void switchToMainView(Stage stage, boolean offlineMode) {
        try {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void switchToLoginView(Stage stage) {
        try {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}