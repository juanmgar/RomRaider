package com.romraider.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public final class DialogUtils {

    private static final String PATH_ROMRAIDER_STYLES = "/styles/romraider.css";
    private static final String PATH_ROMRAIDER_ICON = "/assets/romraider-icon.png";

    /**
     * Representa un diálogo cargado desde FXML: stage + controller.
     */
    public static final class Dialog<T> {
        private final Stage stage;
        private final T controller;

        private Dialog(Stage stage, T controller) {
            this.stage = stage;
            this.controller = controller;
        }

        public Stage getStage() {
            return stage;
        }

        public T getController() {
            return controller;
        }

        public void show() {
            stage.show();
        }

        public void showAndWait() {
            stage.showAndWait();
        }
    }

    /**
     * Carga un FXML, crea un Stage modal con estilos e icono, y devuelve dicho
     * Stage junto con su controller.
     *
     * @param fxmlPath    ruta al FXML (ej: "/views/RomFormView.fxml").
     * @param title       título de la ventana.
     * @param owner       ventana padre (puede ser null).
     * @param resizable   si la ventana es redimensionable.
     * @param applyStyles si se debe aplicar la hoja de estilos romraider.css.
     */
    public static <T> Dialog<T> createDialog(String fxmlPath,
                                             String title,
                                             Stage owner,
                                             boolean resizable,
                                             boolean applyStyles) throws IOException {

        FXMLLoader loader = new FXMLLoader(DialogUtils.class.getResource(fxmlPath));
        Parent root = loader.load();
        T controller = loader.getController();

        Stage stage = new Stage();
        stage.setTitle(title);
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setResizable(resizable);

        Scene scene = new Scene(root);
        if (applyStyles) {
            scene.getStylesheets().add(
                    DialogUtils.class.getResource(PATH_ROMRAIDER_STYLES).toExternalForm()
            );
        }
        stage.getIcons().add(
                new Image(DialogUtils.class.getResourceAsStream(PATH_ROMRAIDER_ICON))
        );
        stage.setScene(scene);

        return new Dialog<>(stage, controller);
    }
}
