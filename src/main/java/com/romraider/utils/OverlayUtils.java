package com.romraider.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class OverlayUtils {

    private OverlayUtils() {
        // Utility class
    }

    /**
     * Crea y muestra un overlay con fondo semitransparente y un spinner centrado.
     *
     * @param root    Root principal de la escena (normalmente un BorderPane, VBox, etc. que extiende de Pane).
     * @param message Mensaje a mostrar bajo el spinner.
     * @return El StackPane del overlay, para poder ocultarlo posteriormente.
     */
    public static StackPane showLoading(Pane root, String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(80, 80);

        VBox content = new VBox(15, spinner, label);
        content.setAlignment(Pos.CENTER);

        StackPane overlay = new StackPane(content);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6)");
        StackPane.setAlignment(content, Pos.CENTER);

        root.getChildren().add(overlay);
        return overlay;
    }

    /**
     * Oculta/elimina el overlay indicado del root.
     */
    public static void hideLoading(Pane root, StackPane overlay) {
        if (root != null && overlay != null) {
            root.getChildren().remove(overlay);
        }
    }
}
