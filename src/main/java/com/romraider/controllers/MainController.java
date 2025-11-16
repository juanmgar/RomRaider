package com.romraider.controllers;

import com.romraider.api.RawgApiClient;
import com.romraider.api.SupabaseAuthService;
import com.romraider.api.SupabaseSyncService;
import com.romraider.auth.SessionManager;
import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final String PATH_ROMRAIDER_STYLES = "/styles/romraider.css";
    private static final String PATH_ROMRAIDER_ICON = "/assets/romraider-icon.png";

    private final PlataformaService plataformaService = new PlataformaService();
    private final RomService romService = new RomService();

    private Plataforma plataformaSeleccionada;
    private List<Rom> roms;
    private ObservableList<String> romTitulos = FXCollections.observableArrayList();
    private FilteredList<String> romFiltradas;

    @FXML
    private MenuBar menuBar;
    @FXML
    private ListView<Plataforma> platformListView;
    @FXML
    private ListView<String> romListView;
    @FXML
    private TextArea romDescription;
    @FXML
    private CheckBox favoriteCheckBox;
    @FXML
    private CheckBox playedCheckBox;
    @FXML
    private MenuItem loginLogoutMenuItem;
    @FXML
    private TextField searchField;
    @FXML
    private ImageView romImage;
    @FXML
    private Button syncButton;
    @FXML
    private Label userLabel;
    @FXML
    private Label syncLabel;

    @FXML
    public void initialize() {
        logger.info("Main view initialized");
        boolean online = NetworkUtils.isInternetAvailable();
        boolean loggedIn = SupabaseAuthService.getCurrentUserEmail() != null;

        // Si no hay conexión o estamos en modo offline, bloquear sincronización
        syncButton.setDisable(!online || !loggedIn);
        userLabel.setText(loggedIn ? SupabaseAuthService.getCurrentUserEmail() : "Offline");

        logger.info("ListView before clear: {}", platformListView);
        if (!online || !loggedIn) {
            platformListView.setDisable(true);
            romListView.setDisable(true);
            platformListView.getItems().clear();
            romListView.getItems().clear();
            syncLabel.setText("Offline mode");
        } else {
            // Mostrar última fecha de sincronización si estamos en modo online
            LocalDateTime lastSync = SyncStateUtils.getLastSync();
            String syncText = (lastSync != null)
                    ? "Last sync: " + lastSync.format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy"))
                    : "Last sync: never";
            syncLabel.setText(syncText);

            cargarPlataformas();
        }

        platformListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                plataformaSeleccionada = selected;
                cargarRomsPorPlataforma(selected);
            }
        });

        romListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                mostrarDetallesRom(selected);
            }
        });
    }

    private void cargarPlataformas() {
        Plataforma todas = new Plataforma();
        todas.setId(-1);
        todas.setNombre("All Platforms");

        List<Plataforma> plataformas = new java.util.ArrayList<>(plataformaService.obtenerTodas());
        plataformas.add(0, todas);
        ObservableList<Plataforma> listaObservable = FXCollections.observableArrayList(plataformas);
        platformListView.setItems(listaObservable);
        platformListView.getSelectionModel().selectFirst();
        plataformaSeleccionada = todas;
        cargarRomsPorPlataforma(todas);

        logger.debug("Loaded {} plataformas", plataformas.size());
    }

    private void cargarRomsPorPlataforma(Plataforma plataforma) {
        if (plataforma.getId() == -1L) {
            roms = new java.util.ArrayList<>(romService.obtenerTodas());
        } else {
            roms = new java.util.ArrayList<>(romService.obtenerPorPlataforma(plataforma.getId()));
        }

        romTitulos.setAll(roms.stream().map(Rom::getTitulo).toList());

        if (romFiltradas == null) {
            romFiltradas = new FilteredList<>(romTitulos, s -> true);
            romListView.setItems(romFiltradas);
        } else {
            romListView.setItems(romFiltradas);
        }
        logger.debug("Loaded {} ROMs for platform {}", roms.size(), plataforma.getNombre());
    }

    private void mostrarDetallesRom(String titulo) {
        Rom rom = roms.stream().filter(r -> r.getTitulo().equals(titulo)).findFirst().orElse(null);
        if (rom != null) {
            romDescription.setText(rom.getDescripcion() != null ? rom.getDescripcion() : "(No description)");
            favoriteCheckBox.setSelected(rom.isFavorito());
            playedCheckBox.setSelected(rom.isJugado());

            String imagen = rom.getImagen();

            if (imagen != null && !imagen.isBlank()) {
                File imageFile = new File(imagen);
                if (imageFile.exists()) {
                    romImage.setImage(new Image("file:" + imageFile.getAbsolutePath(), true));
                } else {
                    romImage.setImage(getDefaultImage());
                }
            } else {
                romImage.setImage(getDefaultImage());
            }

            logger.debug("Details shown for ROM: {}", rom.getTitulo());
        }
    }

    @FXML
    public void handleLoginLogout() {
        logger.info("Logging out and returning to login screen");

        SupabaseAuthService.logout();
        SessionManager.clearSession();

        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneUtils.switchToLoginView(stage);
    }

    @FXML
    public void handleSearch() {
        String filtro = searchField.getText().toLowerCase().trim();
        logger.info("Search changed: {}", filtro);

        if (romFiltradas != null) {
            romFiltradas.setPredicate(titulo -> titulo.toLowerCase().contains(filtro));
        }
    }

    @FXML
    public void handleScanFolder() {
        logger.info("Initiating ROM scan process...");

        PropertyUtils config = AppInitializer.loadConfig();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder to Scan");
        File selectedDir = directoryChooser.showDialog(menuBar.getScene().getWindow());

        if (selectedDir == null || !selectedDir.exists()) {
            logger.warn("No directory selected or it doesn't exist");
            return;
        }

        String baseFolderRelativa = config.get("romraider.roms.default-folder");
        if (baseFolderRelativa == null || baseFolderRelativa.isBlank()) {
            MessageUtils.showError("Default ROM folder not configured.");
            logger.error("Missing configuration: 'romraider.roms.default-folder'");
            return;
        }

        String baseFolder = Paths.get(System.getProperty("user.home"), baseFolderRelativa).toString();

        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        Map<String, Plataforma> extensionMap = plataformas.stream()
                .collect(Collectors.toMap(
                        p -> p.getExtensionRom().toLowerCase(),
                        p -> p
                ));

        try {
            Files.walk(selectedDir.toPath())
                    .filter(Files::isRegularFile)
                    .forEach(path -> processRomFile(path, baseFolder, extensionMap));

            cargarPlataformas();
            MessageUtils.showInfo("Scan completed successfully.");
            logger.info("ROM scan finished");
        } catch (IOException e) {
            logger.error("Error scanning folder", e);
            MessageUtils.showError("Error scanning the selected folder.");
        }
    }

    private void processRomFile(Path path, String baseFolder, Map<String, Plataforma> extensionMap) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String extension = (dotIndex != -1) ? filename.substring(dotIndex).toLowerCase() : "";
        String titulo = (dotIndex != -1) ? filename.substring(0, dotIndex) : filename;

        Plataforma plataforma = extensionMap.get(extension);
        if (plataforma == null) {
            logger.debug("Skipping unsupported file: {}", filename);
            return;
        }

        if (romService.existeRomConTituloYPlataforma(titulo, plataforma.getId())) {
            logger.info("ROM already exists: '{}' for platform '{}'", titulo, plataforma.getNombre());
            return;
        }

        File targetDir = new File(baseFolder, plataforma.getCarpeta());
        targetDir.mkdirs();

        File destFile = new File(targetDir, filename);
        try {
            Files.move(path, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("Moved ROM file: {} → {}", path, destFile);

            Rom rom = new Rom();
            rom.setTitulo(titulo);
            rom.setDescripcion("(Scanned ROM)");
            rom.setFavorito(false);
            rom.setJugado(false);
            rom.setRuta(destFile.getAbsolutePath());
            rom.setImagen(null);
            rom.setPlataforma(plataforma);

            PropertyUtils config = AppInitializer.loadConfig();
            if ("true".equalsIgnoreCase(config.get("romraider.api.autoupdate"))) {
                RawgApiClient.RomInfo info = RawgApiClient.obtenerInfo(titulo);
                if (info != null) {
                    if (info.descripcion != null) {
                        rom.setDescripcion(info.descripcion);
                    }

                    if (info.imageUrl != null) {
                        String localPath = ImageUtils.downloadAndSaveImage(info.imageUrl, titulo, rom.getId());
                        if (localPath != null) {
                            rom.setImagen(localPath);
                        }
                    }

                    logger.info("ROM '{}' updated with RAWG data", titulo);
                }
            }

            romService.guardar(rom);
            logger.info("ROM inserted into database: {}", titulo);
        } catch (IOException e) {
            logger.error("Failed to move file: {}", filename, e);
        }
    }

    @FXML
    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export ROM Collection to XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File file = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try {
                List<Plataforma> plataformas = plataformaService.obtenerTodasConRoms();
                XMLUtils.exportarAxml(plataformas, file);
                MessageUtils.showInfo("Export completed successfully.");
                logger.info("Exported collection to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error exporting XML", e);
                MessageUtils.showError("Failed to export the collection.");
            }
        }
    }

    @FXML
    public void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import XML");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML Files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(menuBar.getScene().getWindow());

        if (selectedFile != null) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Import");
            confirmAlert.setHeaderText("This will delete all existing platforms and ROMs.");
            confirmAlert.setContentText("Are you sure you want to proceed?");
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        List<Plataforma> plataformasImportadas =
                                new ArrayList<>(XMLUtils.importarDesdeXml(selectedFile));

                        plataformaService.eliminarTodas();

                        for (Plataforma plataforma : plataformasImportadas) {
                            // ⚙️ Garantizar que la lista de ROMs sea mutable
                            if (!(plataforma.getRoms() instanceof java.util.ArrayList)) {
                                plataforma.setRoms(new ArrayList<>(plataforma.getRoms()));
                            }

                            for (Rom rom : plataforma.getRoms()) {
                                rom.setPlataforma(plataforma);
                            }

                            plataformaService.guardar(plataforma);
                        }

                        cargarPlataformas();
                        romListView.setItems(FXCollections.observableArrayList());
                        MessageUtils.showInfo("Import completed successfully.");
                        logger.info("Imported collection from: {}", selectedFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.error("Error importing XML", e);
                        MessageUtils.showError("Error importing XML: " + e.getMessage());
                    }
                }
            });
        }
    }


    /**
     * Actualiza una única ROM seleccionada usando la API de RAWG.io.
     * <p>
     * Se obtiene la información de la ROM (descripción e imagen),
     * se actualiza la base de datos y se refresca la vista.
     */
    @FXML
    public void handleUpdateFromAPI() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showInfo("Please select a ROM to update.");
            return;
        }

        // Buscar ROM seleccionada en la lista en memoria
        Rom rom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (rom == null) {
            logger.warn("No se encontró la ROM '{}' en la lista actual", selectedTitle);
            return;
        }

        logger.info("Solicitando datos RAWG.io para '{}'", rom.getTitulo());
        RawgApiClient.RomInfo info = RawgApiClient.obtenerInfo(rom.getTitulo());

        try {
            if (info != null && info.descripcion != null) {

                // Actualizar descripción
                rom.setDescripcion(info.descripcion);
                logger.info("Descripción actualizada correctamente");

                // Descargar imagen si existe
                if (info.imageUrl != null) {
                    String localPath =
                            ImageUtils.downloadAndSaveImage(info.imageUrl, rom.getTitulo(), rom.getId());

                    if (localPath != null) {
                        rom.setImagen(localPath);
                        logger.info("Imagen descargada y almacenada en '{}'", localPath);
                    }
                }

                romService.guardar(rom);
                mostrarDetallesRom(rom.getTitulo());

                MessageUtils.showInfo("ROM data updated from RAWG.io");

            } else {
                MessageUtils.showWarning("No data found on RAWG.io");
            }

        } catch (Exception e) {
            logger.error("Error descargando o procesando datos de RAWG.io", e);
            MessageUtils.showError("Failed to download image from RAWG.io: " + e.getMessage());
        }
    }

    /**
     * Actualiza TODAS las ROMs almacenadas usando RAWG.io
     * ejecutándose en un hilo en segundo plano.
     * <p>
     * Muestra un overlay con spinner durante el proceso.
     * Al finalizar, muestra un resumen con los resultados.
     */
    @FXML
    public void handleUpdateAllFromAPI() {

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Update All ROMs");
        confirmAlert.setHeaderText("This will update all ROM descriptions and images from RAWG.io.");
        confirmAlert.setContentText("Do you want to continue?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            logger.info("Iniciando actualización masiva desde RAWG.io...");

            List<Rom> allRoms = romService.obtenerTodas();
            if (allRoms.isEmpty()) {
                MessageUtils.showInfo("No ROMs found in the local database.");
                return;
            }

            // Crear overlay con spinner semitransparente
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setPrefSize(80, 80);

            StackPane overlay = new StackPane(spinner);
            overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6)");
            StackPane.setAlignment(spinner, Pos.CENTER);

            Pane root = (Pane) menuBar.getScene().getRoot();
            root.getChildren().add(overlay);

            // Bloquear los botones principales durante la operación
            syncButton.setDisable(true);

            // Tarea en segundo plano para evitar bloquear la UI
            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() {
                    int total = allRoms.size();
                    int updated = 0;
                    int notFound = 0;

                    for (Rom rom : allRoms) {
                        try {
                            logger.info("Obteniendo datos de RAWG.io para '{}'", rom.getTitulo());
                            RawgApiClient.RomInfo info = RawgApiClient.obtenerInfo(rom.getTitulo());

                            if (info != null && info.descripcion != null) {
                                rom.setDescripcion(info.descripcion);
                                logger.info("Descripción actualizada para '{}'", rom.getTitulo());

                                if (info.imageUrl != null) {
                                    String localPath = ImageUtils.downloadAndSaveImage(
                                            info.imageUrl,
                                            rom.getTitulo(),
                                            rom.getId()
                                    );
                                    if (localPath != null) {
                                        rom.setImagen(localPath);
                                        logger.info("Imagen actualizada para '{}'", rom.getTitulo());
                                    }
                                }

                                romService.guardar(rom);
                                updated++;

                            } else {
                                notFound++;
                                logger.warn("Sin datos en RAWG.io para '{}'", rom.getTitulo());
                            }

                        } catch (Exception e) {
                            logger.error("Error actualizando '{}'", rom.getTitulo(), e);
                        }
                    }

                    // Resultados finales pasados al hilo de UI
                    final int fUpdated = updated;
                    final int fNotFound = notFound;
                    final int fTotal = total;

                    Platform.runLater(() -> {
                        root.getChildren().remove(overlay);
                        syncButton.setDisable(false);

                        String summary = String.format(
                                "Bulk update completed.\nUpdated: %d\nNot found: %d\nTotal: %d",
                                fUpdated, fNotFound, fTotal
                        );

                        MessageUtils.showInfo(summary);

                        logger.info(
                                "Actualización RAWG.io finalizada: {}/{} actualizadas ({} sin datos)",
                                fUpdated, fTotal, fNotFound
                        );

                        cargarPlataformas();
                    });

                    return null;
                }
            };

            updateTask.setOnFailed(e -> {
                Platform.runLater(() -> {
                    root.getChildren().remove(overlay);
                    syncButton.setDisable(false);
                    MessageUtils.showError("RAWG.io update failed: " + updateTask.getException().getMessage());
                });
            });

            new Thread(updateTask).start();
        });
    }

    /**
     * Realiza la sincronización con Supabase.
     * <p>
     * La sincronización se ejecuta en segundo plano y mientras tanto
     * se muestra un overlay semitransparente con un spinner.
     */
    @FXML
    public void handleSync() {

        // Overlay semitransparente para bloquear interacción
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(80, 80);

        StackPane overlay = new StackPane(spinner);
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6)");
        StackPane.setAlignment(spinner, Pos.CENTER);

        Pane root = (Pane) menuBar.getScene().getRoot();
        root.getChildren().add(overlay);
        syncButton.setDisable(true);

        // Hilo en segundo plano de sincronización
        Task<Void> syncTask = new Task<>() {
            @Override
            protected Void call() {
                logger.info("Iniciando sincronización con Supabase...");
                SupabaseSyncService.syncWithSupabase();
                return null;
            }
        };

        syncTask.setOnSucceeded(e -> {
            root.getChildren().remove(overlay);
            syncButton.setDisable(false);

            LocalDateTime lastSync = SyncStateUtils.getLastSync();
            if (lastSync != null) {
                syncLabel.setText("Last sync: " +
                        lastSync.format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy")));
            }

            SoundUtils.play(SoundUtils.UPLOAD);
            MessageUtils.showInfo("Synchronization complete.");
            logger.info("Sincronización completada con éxito");
        });

        syncTask.setOnFailed(e -> {
            root.getChildren().remove(overlay);
            syncButton.setDisable(false);

            MessageUtils.showError("Synchronization failed: " + syncTask.getException().getMessage());
            logger.error("La sincronización falló", syncTask.getException());
        });

        new Thread(syncTask).start();
    }

    /**
     * Abre la ventana de configuración (PreferencesView.fxml).
     */
    @FXML
    public void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PreferencesView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Preferences");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    SceneUtils.class.getResource(PATH_ROMRAIDER_STYLES).toExternalForm()
            );

            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(PATH_ROMRAIDER_ICON)));
            stage.setScene(scene);
            stage.showAndWait();

            logger.info("Ventana de preferencias abierta");

        } catch (IOException e) {
            logger.error("Error al abrir ventana de preferencias", e);
        }
    }

    /**
     * Abre la ventana de estadísticas de ROMs.
     */
    @FXML
    public void handleViewStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/StatisticsView.fxml"));
            Parent statisticsRoot = loader.load();

            Scene statScene = new Scene(statisticsRoot);
            statScene.getStylesheets().add(
                    getClass().getResource(PATH_ROMRAIDER_STYLES).toExternalForm()
            );

            Stage stage = new Stage();
            stage.setTitle("Statistics");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(statScene);
            stage.show();

        } catch (IOException e) {
            logger.error("Error al cargar la vista de estadísticas", e);
        }
    }

    /**
     * Abre el formulario para añadir una nueva plataforma.
     */
    @FXML
    public void handleAddPlatform() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PlataformaFormView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add Platform");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(getClass().getResourceAsStream(PATH_ROMRAIDER_ICON)));
            stage.showAndWait();

            cargarPlataformas();

        } catch (IOException e) {
            logger.error("Error al abrir formulario de plataforma", e);
            MessageUtils.showError("Could not open the Platform form.");
        }
    }

    /**
     * Abre el formulario para editar una plataforma existente.
     */
    @FXML
    public void handleEditPlatform() {
        Plataforma selected = platformListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            MessageUtils.showWarning("Please select a platform to edit.");
            return;
        }

        if (selected.getId() == -1) {
            MessageUtils.showWarning("You cannot edit the 'All' platform.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PlataformaFormView.fxml"));
            Parent root = loader.load();

            PlataformaFormController controller = loader.getController();
            controller.setPlataformaToEdit(selected);

            Stage stage = new Stage();
            stage.setTitle("Edit Platform");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(getClass().getResourceAsStream(PATH_ROMRAIDER_ICON)));
            stage.showAndWait();

            cargarPlataformas();
            if (platformListView.getItems().contains(selected)) {
                platformListView.getSelectionModel().select(selected);
            }

        } catch (IOException e) {
            logger.error("Error al abrir edición de plataforma", e);
            MessageUtils.showError("Could not open the Platform form.");
        }
    }

    /**
     * Permite eliminar una plataforma, incluyendo todas sus ROMs asociadas.
     */
    @FXML
    public void handleDeletePlatform() {
        Plataforma selected = platformListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            logger.warn("Intento de eliminación sin plataforma seleccionada");
            return;
        }

        if (selected.getId() == -1) {
            MessageUtils.showWarning("You cannot delete the 'All' platform.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete platform '" + selected.getNombre() + "'?");
        alert.setContentText("All associated ROMs will also be deleted. Continue?");

        alert.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            try {
                romService.eliminarPorPlataforma(selected.getId());
                plataformaService.eliminar(selected.getId());

                cargarPlataformas();

                romListView.setItems(FXCollections.observableArrayList());
                platformListView.getSelectionModel().selectFirst();

                logger.info("Plataforma eliminada correctamente: {}", selected.getNombre());

            } catch (Exception e) {
                logger.error("Error eliminando plataforma", e);
                MessageUtils.showError("Could not delete platform: " + e.getMessage());
            }
        });
    }

    /**
     * Abre el formulario para añadir una ROM.
     */
    @FXML
    public void handleAddRom() {
        if (plataformaSeleccionada == null) {
            MessageUtils.showWarning("Please select a platform first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RomFormView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Add ROM");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(PATH_ROMRAIDER_ICON)));
            stage.showAndWait();

            cargarRomsPorPlataforma(plataformaSeleccionada);

        } catch (IOException e) {
            logger.error("Error abriendo formulario de ROM", e);
            MessageUtils.showError("Could not open the ROM form.");
        }
    }

    /**
     * Permite editar una ROM seleccionada.
     */
    @FXML
    public void handleEditRom() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showInfo("Please select a ROM to edit.");
            return;
        }

        Rom selectedRom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (selectedRom == null) {
            MessageUtils.showWarning("Selected ROM not found.");
            return;
        }

        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/views/RomFormView.fxml"));

            Parent root = loader.load();
            RomFormController controller = loader.getController();
            controller.setRomToEdit(selectedRom);

            Stage stage = new Stage();
            stage.setTitle("Edit ROM");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(PATH_ROMRAIDER_ICON)));
            stage.showAndWait();

            cargarRomsPorPlataforma(plataformaSeleccionada);
            mostrarDetallesRom(selectedRom.getTitulo());

        } catch (IOException e) {
            logger.error("Error abriendo edición de ROM", e);
            MessageUtils.showError("Could not open the ROM form.");
        }
    }

    /**
     * Elimina la ROM seleccionada de la base de datos.
     */
    @FXML
    public void handleDeleteRom() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();

        if (selectedTitle == null) {
            MessageUtils.showWarning("Please select a ROM to delete.");
            return;
        }

        Rom selectedRom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (selectedRom == null) {
            MessageUtils.showWarning("Selected ROM not found.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete ROM '" + selectedRom.getTitulo() + "'?");
        alert.setContentText("This action cannot be undone. Are you sure?");

        alert.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            romService.eliminar(selectedRom.getId());
            logger.info("ROM eliminada: {}", selectedRom.getTitulo());

            cargarRomsPorPlataforma(plataformaSeleccionada);
            romDescription.clear();
            favoriteCheckBox.setSelected(false);
            playedCheckBox.setSelected(false);
            romImage.setImage(getDefaultImage());

            MessageUtils.showInfo("ROM deleted successfully.");
        });
    }

    /**
     * Muestra la ventana de créditos del proyecto.
     */
    @FXML
    public void handleCredits() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CreditsView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Credits");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    SceneUtils.class.getResource(PATH_ROMRAIDER_STYLES).toExternalForm()
            );

            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream(PATH_ROMRAIDER_ICON)));
            stage.setScene(scene);
            stage.showAndWait();

            logger.info("Ventana de créditos abierta correctamente");

        } catch (IOException e) {
            logger.error("Error abriendo ventana de créditos", e);
            MessageUtils.showError("Could not open the Credits window.");
        }
    }

    @FXML
    public void handleOpenRomFolder() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showWarning("Please select a ROM first.");
            return;
        }

        Rom rom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (rom == null || rom.getRuta() == null || rom.getRuta().isBlank()) {
            MessageUtils.showWarning("ROM path not available.");
            return;
        }

        File romFile = new File(rom.getRuta());
        File folder = romFile.getParentFile();

        if (folder == null || !folder.exists()) {
            MessageUtils.showError("ROM folder not found:\n" + rom.getRuta());
            return;
        }

        try {
            // WINDOWS
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("explorer.exe", folder.getAbsolutePath()).start();
            }
            // macOS
            else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                new ProcessBuilder("open", folder.getAbsolutePath()).start();
            }
            // Linux
            else {
                new ProcessBuilder("xdg-open", folder.getAbsolutePath()).start();
            }

            logger.info("Opened ROM folder: {}", folder.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Could not open folder", e);
            MessageUtils.showError("Could not open folder:\n" + e.getMessage());
        }
    }


    private Image getDefaultImage() {
        return new Image(getClass().getResourceAsStream("/assets/no-image.png"));
    }
}
