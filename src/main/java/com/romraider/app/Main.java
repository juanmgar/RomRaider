package com.romraider.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        stage.setTitle("ROM Raider");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/romraider-icon.png")));
        scene.getStylesheets().add(getClass().getResource("/styles/romraider.css").toExternalForm());
        stage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}
