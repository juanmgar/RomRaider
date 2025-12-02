package com.romraider.controllers;

import com.romraider.api.SupabaseAuthService;
import com.romraider.api.SupabaseSyncService;
import com.romraider.app.AppInitializer;
import com.romraider.auth.SessionManager;
import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RawgRomUpdateService;
import com.romraider.service.RomService;
import com.romraider.utils.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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
import java.util.*;
import java.util.stream.Collectors;

import static com.romraider.db.DataInitializer.insertOrUpdateDefaultPlatforms;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final PlataformaService plataformaService = new PlataformaService();
    private final RomService romService = new RomService();
    private final RawgRomUpdateService rawgRomUpdateService = new RawgRomUpdateService(romService);

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
    private Label romPlatformLabel;
    @FXML
    private MenuItem loginLogoutMenuItem;

    @FXML
    public void initialize() {
        logger.info("Main view initialized");
        boolean online = NetworkUtils.isInternetAvailable();
        boolean loggedIn = SupabaseAuthService.getCurrentUserEmail() != null;

        // Si no hay conexión o estamos en modo offline, bloquear sincronización
        syncButton.setDisable(!online || !loggedIn);
        userLabel.setText(loggedIn
                ? SupabaseAuthService.getCurrentUserEmail()
                : I18nUtils.get("main.status.offline"));

        updateLoginLogoutMenu();

        logger.info("ListView before clear: {}", platformListView);
        if (!online || !loggedIn) {
            syncLabel.setText(I18nUtils.get("main.status.offlineMode"));
            insertOrUpdateDefaultPlatforms();
            cargarPlataformas();
        } else {
            // Mostrar última fecha de sincronización si estamos en modo online
            LocalDateTime lastSync = SyncStateUtils.getLastSync();
            String syncText = (lastSync != null)
                    ? I18nUtils.get("main.sync.lastPrefix")
                    + lastSync.format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy"))
                    : I18nUtils.get("main.sync.never");
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
        todas.setNombre(I18nUtils.get("main.platform.all"));

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

        List<String> titulosOrdenados = roms.stream()
                .map(Rom::getTitulo)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        romTitulos.setAll(titulosOrdenados);

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
            romDescription.setText(
                    rom.getDescripcion() != null
                            ? rom.getDescripcion()
                            : I18nUtils.get("main.rom.noDescription")
            );

            if (rom.getPlataforma() != null) {
                romPlatformLabel.setText(rom.getPlataforma().getNombre());
            } else {
                romPlatformLabel.setText(I18nUtils.get("main.platform.none"));
            }

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

        File selectedDir = validarDirectorioSeleccionado();
        if (selectedDir == null) return;

        String baseFolderRelativa = AppInitializer.loadConfig().get("romraider.roms.default-folder");
        if (baseFolderRelativa == null || baseFolderRelativa.isBlank()) {
            MessageUtils.showError(I18nUtils.get("main.scan.error.noDefaultFolder"));
            logger.error("Missing configuration: 'romraider.roms.default-folder'");
            return;
        }

        String baseFolder = Paths.get(System.getProperty("user.home"), baseFolderRelativa).toString();

        Pane root = (Pane) menuBar.getScene().getRoot();
        StackPane overlay = OverlayUtils.showLoading(root, I18nUtils.get("main.scan.overlay"));

        Task<String> scanTask =
                crearTaskEscaneo(selectedDir.toPath(), baseFolder);

        scanTask.setOnSucceeded(e -> {
            OverlayUtils.hideLoading(root, overlay);
            cargarPlataformas();
            MessageUtils.showInfo(scanTask.getValue());
        });

        scanTask.setOnFailed(e -> {
            OverlayUtils.hideLoading(root, overlay);
            MessageUtils.showError(I18nUtils.get("main.scan.failedPrefix")
                    + scanTask.getException().getMessage());
        });

        new Thread(scanTask).start();
    }

    private void updateLoginLogoutMenu() {
        boolean loggedIn = SupabaseAuthService.getCurrentUserEmail() != null;

        loginLogoutMenuItem.setText(
                I18nUtils.get(loggedIn ? "menu.file.logout" : "menu.file.login")
        );

        userLabel.setText(
                loggedIn
                        ? SupabaseAuthService.getCurrentUserEmail()
                        : I18nUtils.get("main.status.offline")
        );
    }

    private void processRomFile(Path path, String baseFolder, Map<String, Plataforma> extensionMap) {
        try {
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
            Files.move(path, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            logger.info("Moved ROM file: {} -> {}", path, destFile);

            Rom rom = new Rom();
            rom.setTitulo(titulo);
            rom.setDescripcion(I18nUtils.get("main.rom.scannedPlaceholder"));
            rom.setFavorito(false);
            rom.setJugado(false);
            rom.setRuta(destFile.getAbsolutePath());
            rom.setImagen(null);
            rom.setPlataforma(plataforma);

            // Auto-update RAWG
            PropertyUtils config = AppInitializer.loadConfig();
            if ("true".equalsIgnoreCase(config.get("romraider.api.autoupdate"))) {
                rawgRomUpdateService.updateRomFromRawg(rom, false);
            }

            // Guardar en BD
            try {
                romService.guardar(rom);
                logger.info("ROM inserted into database: {}", titulo);
            } catch (Exception dbException) {
                logger.error("ERROR saving ROM '{}': {}", titulo, dbException.getMessage());
            }

        } catch (Exception fatal) {
            logger.error("Unexpected error processing ROM '{}': {}", path.getFileName(), fatal.getMessage());
        }
    }

    @FXML
    public void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nUtils.get("main.export.dialogTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18nUtils.get("main.export.filter.xml"), "*.xml")
        );
        File file = fileChooser.showSaveDialog(menuBar.getScene().getWindow());

        if (file != null) {
            try {
                List<Plataforma> plataformas = plataformaService.obtenerTodasConRoms();
                XMLUtils.exportarAxml(plataformas, file);
                MessageUtils.showInfo(I18nUtils.get("main.export.success"));
                logger.info("Exported collection to: {}", file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Error exporting XML", e);
                MessageUtils.showError(I18nUtils.get("main.export.failed"));
            }
        }
    }

    @FXML
    public void handleImport() {

        File selectedFile = seleccionarArchivoImportacion();
        if (selectedFile == null) return;

        if (!mostrarDialogoConfirmacion()) return;

        logger.info("Starting XML import with spinner...");

        Pane root = (Pane) menuBar.getScene().getRoot();
        StackPane overlay = OverlayUtils.showLoading(root, I18nUtils.get("main.import.overlay"));

        Task<Void> importTask = crearImportTask(selectedFile);

        importTask.setOnSucceeded(ev -> postImportSuccess(selectedFile, overlay));
        importTask.setOnFailed(ev -> postImportFailure(importTask.getException(), overlay));

        new Thread(importTask).start();
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
            MessageUtils.showInfo(I18nUtils.get("main.updateRom.selectFirst"));
            return;
        }

        Rom rom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (rom == null) {
            logger.warn("No se encontró la ROM '{}' en la lista actual", selectedTitle);
            return;
        }

        RawgRomUpdateService.UpdateResult result =
                rawgRomUpdateService.updateRomFromRawg(rom, true);

        switch (result.getStatus()) {
            case UPDATED -> {
                mostrarDetallesRom(rom.getTitulo());
                MessageUtils.showInfo(I18nUtils.get("main.updateRom.success"));
            }
            case NOT_FOUND -> MessageUtils.showWarning(I18nUtils.get("main.updateRom.notFound"));
            case ERROR -> MessageUtils.showError(
                    I18nUtils.get("main.updateRom.errorPrefix") + result.getErrorMessage()
            );
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
        confirmAlert.setTitle(I18nUtils.get("main.updateAll.title"));
        confirmAlert.setHeaderText(I18nUtils.get("main.updateAll.header"));
        confirmAlert.setContentText(I18nUtils.get("main.updateAll.content"));

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            logger.info("Iniciando actualización masiva desde RAWG.io...");

            List<Rom> allRoms = romService.obtenerTodas();
            if (allRoms.isEmpty()) {
                MessageUtils.showInfo(I18nUtils.get("main.updateAll.noRoms"));
                return;
            }

            Pane root = (Pane) menuBar.getScene().getRoot();
            StackPane overlay = OverlayUtils.showLoading(root, I18nUtils.get("main.updateAll.overlay"));

            syncButton.setDisable(true);

            Task<Void> updateTask = new Task<>() {
                @Override
                protected Void call() {
                    int total = allRoms.size();
                    int updated = 0;
                    int notFound = 0;

                    for (Rom rom : allRoms) {
                        RawgRomUpdateService.UpdateResult result =
                                rawgRomUpdateService.updateRomFromRawg(rom, true);

                        if (result.getStatus() == RawgRomUpdateService.Status.UPDATED) {
                            updated++;
                        } else if (result.getStatus() == RawgRomUpdateService.Status.NOT_FOUND) {
                            notFound++;
                        }
                    }

                    int fUpdated = updated;
                    int fNotFound = notFound;
                    int fTotal = total;

                    Platform.runLater(() -> {
                        OverlayUtils.hideLoading(root, overlay);
                        syncButton.setDisable(false);

                        String summary = String.format(
                                I18nUtils.get("main.updateAll.summary"),
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
                    OverlayUtils.hideLoading(root, overlay);
                    syncButton.setDisable(false);
                    MessageUtils.showError(
                            I18nUtils.get("main.updateAll.errorPrefix")
                                    + updateTask.getException().getMessage()
                    );
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

        Pane root = (Pane) menuBar.getScene().getRoot();
        StackPane overlay = OverlayUtils.showLoading(root, I18nUtils.get("main.sync.overlay"));
        syncButton.setDisable(true);

        Task<Void> syncTask = new Task<>() {
            @Override
            protected Void call() {
                logger.info("Iniciando sincronización con Supabase...");
                SupabaseSyncService.syncWithSupabase();
                return null;
            }
        };

        syncTask.setOnSucceeded(e -> {
            OverlayUtils.hideLoading(root, overlay);
            syncButton.setDisable(false);

            LocalDateTime lastSync = SyncStateUtils.getLastSync();
            if (lastSync != null) {
                syncLabel.setText(
                        I18nUtils.get("main.sync.lastPrefix")
                                + lastSync.format(DateTimeFormatter.ofPattern("HH:mm dd-MM-yyyy"))
                );
            }

            SoundUtils.play(SoundUtils.UPLOAD);
            MessageUtils.showInfo(I18nUtils.get("main.sync.success"));
            logger.info("Sincronización completada con éxito");
        });

        syncTask.setOnFailed(e -> {
            OverlayUtils.hideLoading(root, overlay);
            syncButton.setDisable(false);

            MessageUtils.showError(
                    I18nUtils.get("main.sync.failedPrefix") + syncTask.getException().getMessage()
            );
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
            Stage owner = (Stage) menuBar.getScene().getWindow();
            DialogUtils.Dialog<?> dialog = DialogUtils.createDialog(
                    "/views/PreferencesView.fxml",
                    I18nUtils.get("preferences.title"),
                    owner,
                    false,
                    true
            );
            dialog.showAndWait();
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
            Stage owner = (Stage) menuBar.getScene().getWindow();
            DialogUtils.Dialog<?> dialog = DialogUtils.createDialog(
                    "/views/StatisticsView.fxml",
                    I18nUtils.get("statistics.title"),
                    owner,
                    false,
                    true
            );
            dialog.show();
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
            Stage owner = (Stage) menuBar.getScene().getWindow();
            DialogUtils.Dialog<?> dialog = DialogUtils.createDialog(
                    "/views/PlataformaFormView.fxml",
                    I18nUtils.get("platformForm.add.title"),
                    owner,
                    false,
                    false
            );
            dialog.showAndWait();
            cargarPlataformas();

        } catch (IOException e) {
            logger.error("Error al abrir formulario de plataforma", e);
            MessageUtils.showError(I18nUtils.get("platformForm.error.open"));
        }
    }

    /**
     * Abre el formulario para editar una plataforma existente.
     */
    @FXML
    public void handleEditPlatform() {
        Plataforma selected = platformListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            MessageUtils.showWarning(I18nUtils.get("platformForm.edit.selectFirst"));
            return;
        }

        if (selected.getId() == -1) {
            MessageUtils.showWarning(I18nUtils.get("platformForm.edit.cannotEditAll"));
            return;
        }

        try {
            Stage owner = (Stage) menuBar.getScene().getWindow();

            DialogUtils.Dialog<PlataformaFormController> dialog =
                    DialogUtils.createDialog(
                            "/views/PlataformaFormView.fxml",
                            I18nUtils.get("platformForm.edit.title"),
                            owner,
                            false,
                            false
                    );

            PlataformaFormController controller = dialog.getController();
            controller.setPlataformaToEdit(selected);

            dialog.showAndWait();

            cargarPlataformas();
            if (platformListView.getItems().contains(selected)) {
                platformListView.getSelectionModel().select(selected);
            }

        } catch (IOException e) {
            logger.error("Error al abrir edición de plataforma", e);
            MessageUtils.showError(I18nUtils.get("platformForm.error.open"));
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
            MessageUtils.showWarning(I18nUtils.get("platformForm.delete.cannotDeleteAll"));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.get("platformForm.delete.confirm.title"));
        alert.setHeaderText(
                String.format(I18nUtils.get("platformForm.delete.confirm.header"), selected.getNombre())
        );
        alert.setContentText(I18nUtils.get("platformForm.delete.confirm.content"));

        alert.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            try {
                List<Rom> romsToDelete = romService.obtenerPorPlataforma(selected.getId());

                romsToDelete.forEach(rom -> {
                    if (rom.getImagen() != null && !rom.getImagen().isBlank()) {
                        ImageUtils.deleteImageIfExists(rom.getImagen());
                    }
                });

                romService.eliminarPorPlataforma(selected.getId());
                plataformaService.eliminar(selected.getId());

                cargarPlataformas();

                romListView.setItems(FXCollections.observableArrayList());
                platformListView.getSelectionModel().selectFirst();

                logger.info("Plataforma eliminada correctamente: {}", selected.getNombre());

            } catch (Exception e) {
                logger.error("Error eliminando plataforma", e);
                MessageUtils.showError(
                        I18nUtils.get("platformForm.delete.errorPrefix") + e.getMessage()
                );
            }
        });
    }

    /**
     * Abre el formulario para añadir una ROM.
     */
    @FXML
    public void handleAddRom() {
        if (plataformaSeleccionada == null) {
            MessageUtils.showWarning(I18nUtils.get("romForm.add.selectPlatformFirst"));
            return;
        }

        try {
            Stage owner = (Stage) menuBar.getScene().getWindow();

            DialogUtils.Dialog<RomFormController> dialog =
                    DialogUtils.createDialog(
                            "/views/RomFormView.fxml",
                            I18nUtils.get("romForm.add.title"),
                            owner,
                            false,
                            false
                    );

            dialog.showAndWait();

            cargarRomsPorPlataforma(plataformaSeleccionada);

        } catch (IOException e) {
            logger.error("Error abriendo formulario de ROM", e);
            MessageUtils.showError(I18nUtils.get("romForm.error.open"));
        }
    }

    /**
     * Permite editar una ROM seleccionada.
     */
    @FXML
    public void handleEditRom() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showInfo(I18nUtils.get("romForm.edit.selectRomFirst"));
            return;
        }

        Rom selectedRom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (selectedRom == null) {
            MessageUtils.showWarning(I18nUtils.get("romForm.error.notFound"));
            return;
        }

        try {
            Stage owner = (Stage) menuBar.getScene().getWindow();

            DialogUtils.Dialog<RomFormController> dialog =
                    DialogUtils.createDialog(
                            "/views/RomFormView.fxml",
                            I18nUtils.get("romForm.edit.title"),
                            owner,
                            false,
                            false
                    );

            RomFormController controller = dialog.getController();
            controller.setRomToEdit(selectedRom);

            dialog.showAndWait();

            cargarRomsPorPlataforma(plataformaSeleccionada);
            mostrarDetallesRom(selectedRom.getTitulo());

        } catch (IOException e) {
            logger.error("Error abriendo edición de ROM", e);
            MessageUtils.showError(I18nUtils.get("romForm.error.open"));
        }
    }

    /**
     * Elimina la ROM seleccionada de la base de datos.
     */
    @FXML
    public void handleDeleteRom() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();

        if (selectedTitle == null) {
            MessageUtils.showWarning(I18nUtils.get("romForm.delete.selectRomFirst"));
            return;
        }

        Rom selectedRom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (selectedRom == null) {
            MessageUtils.showWarning(I18nUtils.get("romForm.error.notFound"));
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.get("romForm.delete.confirm.title"));
        alert.setHeaderText(
                String.format(I18nUtils.get("romForm.delete.confirm.header"), selectedRom.getTitulo())
        );
        alert.setContentText(I18nUtils.get("romForm.delete.confirm.content"));

        alert.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            romService.eliminar(selectedRom.getId());
            ImageUtils.deleteImageIfExists(selectedRom.getImagen());
            logger.info("ROM eliminada: {}", selectedRom.getTitulo());

            cargarRomsPorPlataforma(plataformaSeleccionada);
            romDescription.clear();
            favoriteCheckBox.setSelected(false);
            playedCheckBox.setSelected(false);
            romImage.setImage(getDefaultImage());

            MessageUtils.showInfo(I18nUtils.get("romForm.delete.success"));
        });
    }

    /**
     * Muestra la ventana de créditos del proyecto.
     */
    @FXML
    public void handleCredits() {
        try {
            Stage owner = (Stage) menuBar.getScene().getWindow();

            DialogUtils.Dialog<?> dialog =
                    DialogUtils.createDialog(
                            "/views/CreditsView.fxml",
                            I18nUtils.get("credits.title"),
                            owner,
                            false,
                            true
                    );

            dialog.showAndWait();
            logger.info("Ventana de créditos abierta correctamente");

        } catch (IOException e) {
            logger.error("Error abriendo ventana de créditos", e);
            MessageUtils.showError(I18nUtils.get("credits.error.open"));
        }
    }

    /**
     * Muestra la ventana del manual de ayuda.
     */
    @FXML
    public void handleHelp() {
        try {
            DialogUtils.Dialog<?> dialog = DialogUtils.createDialog(
                    "/views/HelpManualView.fxml",
                    I18nUtils.get("help.manual.title"),
                    null,
                    true,
                    true
            );

            dialog.show();
            logger.info("Ventana de ayuda abierta correctamente");

        } catch (Exception e) {
            logger.error("Error al abrir la ventana de ayuda", e);
        }
    }

    @FXML
    public void handleOpenRomFolder() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showWarning(I18nUtils.get("romForm.folder.selectRomFirst"));
            return;
        }

        Rom rom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst()
                .orElse(null);

        if (rom == null || rom.getRuta() == null || rom.getRuta().isBlank()) {
            MessageUtils.showWarning(I18nUtils.get("romForm.folder.pathNotAvailable"));
            return;
        }

        File romFile = new File(rom.getRuta());
        File folder = romFile.getParentFile();

        if (folder == null || !folder.exists()) {
            MessageUtils.showError(
                    String.format(I18nUtils.get("romForm.folder.notFound"), rom.getRuta())
            );
            return;
        }

        try {
            if (!java.awt.Desktop.isDesktopSupported()) {
                logger.error("Desktop API is not supported on this platform.");
                MessageUtils.showError(I18nUtils.get("romForm.folder.desktopNotSupported"));
                return;
            }

            java.awt.Desktop.getDesktop().open(folder);

            logger.info("Opened ROM folder: {}", folder.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Could not open folder", e);
            MessageUtils.showError(
                    String.format(I18nUtils.get("romForm.folder.couldNotOpen"), e.getMessage())
            );
        }
    }

    private File validarDirectorioSeleccionado() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(I18nUtils.get("main.scan.selectFolderTitle"));

        File selectedDir = directoryChooser.showDialog(menuBar.getScene().getWindow());

        if (selectedDir == null || !selectedDir.exists()) {
            logger.warn("No directory selected or it doesn't exist");
            return null;
        }
        return selectedDir;
    }

    private String crearResumen(int romsAdded, Set<Plataforma> plataformasAfectadas) {
        String plataformasTexto = plataformasAfectadas.stream()
                .map(Plataforma::getNombre)
                .sorted()
                .collect(Collectors.joining("\n- ", "- ", ""));

        return String.format(
                I18nUtils.get("main.scan.summary"),
                romsAdded,
                plataformasAfectadas.size(),
                plataformasAfectadas.isEmpty()
                        ? I18nUtils.get("main.scan.summary.none")
                        : plataformasTexto
        );
    }

    private Task<String> crearTaskEscaneo(Path selectedDir, String baseFolder) {
        return new Task<>() {
            @Override
            protected String call() {

                List<Plataforma> plataformas = plataformaService.obtenerTodas();
                Map<String, Plataforma> extensionMap = plataformas.stream()
                        .collect(Collectors.toMap(
                                p -> p.getExtensionRom().toLowerCase(),
                                p -> p
                        ));

                int[] romsAdded = {0};
                Set<Plataforma> plataformasAfectadas = new HashSet<>();

                try (var paths = Files.walk(selectedDir)) {
                    paths.filter(Files::isRegularFile)
                            .forEach(path -> procesarArchivo(path, extensionMap, baseFolder,
                                    romsAdded, plataformasAfectadas));
                } catch (IOException e) {
                    logger.error("Error scanning folder", e);
                    throw new RuntimeException(I18nUtils.get("main.scan.error.exception"), e);
                }

                return crearResumen(romsAdded[0], plataformasAfectadas);
            }
        };
    }

    private void procesarArchivo(Path path, Map<String, Plataforma> extensionMap, String baseFolder,
                                 int[] romsAdded, Set<Plataforma> plataformasAfectadas) {

        try {
            String filename = path.getFileName().toString();
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex == -1) return;

            String extension = filename.substring(dotIndex).toLowerCase();
            Plataforma plataforma = extensionMap.get(extension);
            if (plataforma == null) return;

            String titulo = filename.substring(0, dotIndex);
            if (romService.existeRomConTituloYPlataforma(titulo, plataforma.getId())) return;

            processRomFile(path, baseFolder, extensionMap);

            romsAdded[0]++;
            plataformasAfectadas.add(plataforma);

        } catch (Exception ex) {
            logger.error("Error processing file {}", path, ex);
        }
    }

    private File seleccionarArchivoImportacion() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nUtils.get("main.import.dialogTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18nUtils.get("main.import.filter.xml"), "*.xml")
        );
        return fileChooser.showOpenDialog(menuBar.getScene().getWindow());
    }

    private boolean mostrarDialogoConfirmacion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.get("main.import.confirm.title"));
        alert.setHeaderText(I18nUtils.get("main.import.confirm.header"));
        alert.setContentText(I18nUtils.get("main.import.confirm.content"));

        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    private Task<Void> crearImportTask(File selectedFile) {
        return new Task<>() {
            @Override
            protected Void call() {

                try {
                    List<Plataforma> plataformasImportadas =
                            new ArrayList<>(XMLUtils.importarDesdeXml(selectedFile));

                    plataformaService.eliminarTodas();

                    for (Plataforma plataforma : plataformasImportadas) {

                        if (!(plataforma.getRoms() instanceof ArrayList)) {
                            plataforma.setRoms(new ArrayList<>(plataforma.getRoms()));
                        }

                        for (Rom rom : plataforma.getRoms()) {
                            rom.setPlataforma(plataforma);
                        }

                        plataformaService.guardar(plataforma);
                    }

                } catch (Exception e) {
                    logger.error("Error importing XML", e);
                    throw new RuntimeException("Error importing XML: " + e.getMessage(), e);
                }

                return null;
            }
        };
    }

    private void postImportSuccess(File selectedFile, StackPane overlay) {
        OverlayUtils.hideLoading((Pane) menuBar.getScene().getRoot(), overlay);

        romListView.setItems(FXCollections.observableArrayList());
        cargarPlataformas();

        MessageUtils.showInfo(I18nUtils.get("main.import.success"));
        logger.info("Imported collection from: {}", selectedFile.getAbsolutePath());
    }

    private void postImportFailure(Throwable ex, StackPane overlay) {
        OverlayUtils.hideLoading((Pane) menuBar.getScene().getRoot(), overlay);

        logger.error("Import failed", ex);
        MessageUtils.showError(
                I18nUtils.get("main.import.failedPrefix") + ex.getMessage()
        );
    }

    private Image getDefaultImage() {
        return new Image(getClass().getResourceAsStream("/assets/no-image.png"));
    }
}
