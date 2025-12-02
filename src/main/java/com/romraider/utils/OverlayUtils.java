package com.romraider.utils;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Utilidades para mostrar y ocultar overlays de carga (pantalla semitransparente
 * con spinner) sobre contenedores JavaFX.
 * <p>
 * Esta clase está pensada para indicar operaciones en progreso (por ejemplo,
 * llamadas a APIs, procesos de escaneo, sincronización, etc.) bloqueando la
 * interacción del usuario con el contenido subyacente mientras se muestra
 * el spinner.
 */
public final class OverlayUtils {

    /**
     * Constructor privado para evitar la instanciación de la clase de utilidades.
     */
    private OverlayUtils() {
        // Utility class
    }

    /**
     * Crea y muestra un overlay con fondo semitransparente y un spinner centrado.
     * <p>
     * El overlay se añade como hijo del {@code root} recibido, cubriendo todo su área.
     * Se devuelve el propio {@link StackPane} del overlay para poder eliminarlo
     * posteriormente mediante {@link #hideLoading(Pane, StackPane)}.
     *
     * @param root    root principal de la escena (normalmente un {@code BorderPane}, {@code VBox}, etc. que extiende de {@link Pane}).
     * @param message mensaje a mostrar bajo el spinner.
     * @return el {@link StackPane} creado para el overlay, para poder ocultarlo posteriormente.
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
     * Oculta/elimina el overlay indicado del contenedor raíz.
     * <p>
     * Si {@code root} o {@code overlay} son {@code null}, no se realiza ninguna acción.
     *
     * @param root    contenedor del que se eliminará el overlay.
     * @param overlay overlay que se desea ocultar/eliminar.
     */
    public static void hideLoading(Pane root, StackPane overlay) {
        if (root != null && overlay != null) {
            root.getChildren().remove(overlay);
        }
    }
}
