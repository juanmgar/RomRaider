package com.romraider.app;

import com.romraider.db.JpaUtil;
import com.romraider.db.DataInitializer;
import com.romraider.utils.AppInitializer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage splashStage) {
        logger.info("Starting ROM Raider...");

        AppInitializer.initialize();
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

        logger.debug("Splash screen displayed");

        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            splashStage.close();
            logger.debug("Splash screen closed");

            Stage mainStage = new Stage();
            SceneUtils.switchToLoginView(mainStage);
        });
        delay.play();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        JpaUtil.close();
        logger.info("EntityManagerFactory closed and application shutdown complete");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
