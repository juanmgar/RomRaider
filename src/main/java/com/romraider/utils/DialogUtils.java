package com.romraider.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Utilidades para la creación de diálogos modales a partir de archivos FXML.
 * <p>
 * Esta clase proporciona un método genérico para cargar una vista FXML,
 * crear un {@link Stage} modal asociado y devolverlo junto con su controlador.
 */
public final class DialogUtils {

    /**
     * Ruta a la hoja de estilos principal de RomRaider.
     */
    private static final String PATH_ROMRAIDER_STYLES = "/styles/romraider.css";

    /**
     * Ruta al icono de la aplicación RomRaider.
     */
    private static final String PATH_ROMRAIDER_ICON = "/assets/romraider-icon.png";

    /**
     * Representa un diálogo cargado desde FXML: {@link Stage} + controller.
     *
     * @param <T> tipo del controlador asociado al diálogo.
     */
    public static final class Dialog<T> {

        private final Stage stage;
        private final T controller;

        /**
         * Crea una nueva instancia que agrupa un {@link Stage} y su controlador.
         *
         * @param stage      ventana asociada al diálogo.
         * @param controller instancia del controlador cargado desde el FXML.
         */
        private Dialog(Stage stage, T controller) {
            this.stage = stage;
            this.controller = controller;
        }

        /**
         * Devuelve la ventana ({@link Stage}) asociada al diálogo.
         *
         * @return el {@link Stage} del diálogo.
         */
        public Stage getStage() {
            return stage;
        }

        /**
         * Devuelve el controlador asociado al diálogo.
         *
         * @return el controlador del diálogo.
         */
        public T getController() {
            return controller;
        }

        /**
         * Muestra el diálogo de forma no bloqueante.
         * <p>
         * Equivale a llamar a {@link Stage#show()}.
         */
        public void show() {
            stage.show();
        }

        /**
         * Muestra el diálogo de forma bloqueante hasta que se cierre.
         * <p>
         * Equivale a llamar a {@link Stage#showAndWait()}.
         */
        public void showAndWait() {
            stage.showAndWait();
        }
    }

    /**
     * Carga un FXML, crea un {@link Stage} modal con estilos e icono, y devuelve dicho
     * {@link Stage} junto con su controlador.
     *
     * @param fxmlPath    ruta al FXML (ej: {@code "/views/RomFormView.fxml"}).
     * @param title       título de la ventana.
     * @param owner       ventana padre (puede ser {@code null}).
     * @param resizable   si la ventana es redimensionable.
     * @param applyStyles si se debe aplicar la hoja de estilos {@code romraider.css}.
     * @param <T>         tipo del controlador asociado al FXML.
     * @return un objeto {@link Dialog} que contiene el {@link Stage} creado y su controlador.
     * @throws IOException si ocurre un error al cargar el archivo FXML.
     */
    public static <T> Dialog<T> createDialog(String fxmlPath,
                                             String title,
                                             Stage owner,
                                             boolean resizable,
                                             boolean applyStyles) throws IOException {

        FXMLLoader loader = new FXMLLoader(DialogUtils.class.getResource(fxmlPath), I18nUtils.getBundle());
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
