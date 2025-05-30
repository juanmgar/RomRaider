package com.romraider.app;

import com.romraider.db.JpaUtil;
import com.romraider.db.DataInitializer;
import com.romraider.utils.SceneUtils;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Main extends Application {

    @Override
    public void start(Stage splashStage) {
        // Inicializa la base de datos con datos por defecto usando JPA
        DataInitializer.initializeWithDefaults();

        ImageView imageView = new ImageView(
                new Image(getClass().getResource("/assets/romraider-logo.png").toExternalForm())
        );
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(500);

        StackPane root = new StackPane(imageView);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(null);

        splashStage.initStyle(StageStyle.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.setAlwaysOnTop(true);
        splashStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            splashStage.close();
            Stage mainStage = new Stage();
            SceneUtils.switchToLoginView(mainStage);
        });
        delay.play();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        JpaUtil.close();
        System.out.println("EntityManagerFactory closed.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
