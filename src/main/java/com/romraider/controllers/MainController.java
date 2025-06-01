package com.romraider.controllers;

import com.romraider.api.RawgApiClient;
import com.romraider.auth.SessionManager;
import com.romraider.auth.SupabaseAuthService;
import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final PlataformaService plataformaService = new PlataformaService();
    private final RomService romService = new RomService();
    private boolean offlineMode = false;

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
    private Button syncButton;
    @FXML
    private MenuItem loginLogoutMenuItem;
    @FXML
    private TextField searchField;
    @FXML
    private ImageView romImage;

    @FXML
    public void initialize() {
        boolean online = NetworkUtils.isInternetAvailable();
        logger.info("Main view initialized");
        syncButton.setDisable(true);

        if (!online) {
            syncButton.setDisable(true);
        }

        cargarPlataformas();

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

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
        syncButton.setDisable(offlineMode);
        loginLogoutMenuItem.setText(offlineMode ? "Login" : "Logout");
        logger.info("Main view opened in offline mode: {}", offlineMode);
    }

    private void cargarPlataformas() {
        Plataforma todas = new Plataforma();
        todas.setId(-1);
        todas.setNombre("Todas");

        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        plataformas.add(0, todas);

        platformListView.setItems(FXCollections.observableArrayList(plataformas));
        platformListView.getSelectionModel().selectFirst();
        plataformaSeleccionada = todas;
        cargarRomsPorPlataforma(todas);

        logger.debug("Loaded {} plataformas", plataformas.size());
    }

    private void cargarRomsPorPlataforma(Plataforma plataforma) {
        if (plataforma.getId() == -1L) {
            roms = romService.obtenerTodas();
        } else {
            roms = romService.obtenerPorPlataforma(plataforma.getId());
        }

        romTitulos.setAll(roms.stream().map(Rom::getTitulo).toList());

        if (romFiltradas == null) {
            romFiltradas = new FilteredList<>(romTitulos, s -> true);
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

        offlineMode = true;

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

        String baseFolderRelativa  = config.get("romraider.roms.default-folder");
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
                        List<Plataforma> plataformasImportadas = new java.util.ArrayList<>(XMLUtils.importarDesdeXml(selectedFile));

                        plataformaService.eliminarTodas();

                        for (Plataforma plataforma : plataformasImportadas) {
                            for (Rom rom : plataforma.getRoms()) {
                                rom.setPlataforma(plataforma);
                            }
                            plataformaService.guardar(plataforma);
                        }

                        cargarPlataformas();
                        romListView.getItems().clear();
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

    @FXML
    public void handleUpdateFromAPI() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showInfo("Please select a ROM to update.");
            return;
        }

        Rom rom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst().orElse(null);

        if (rom == null) {
            logger.warn("ROM '{}' not found", selectedTitle);
            return;
        }

        logger.info("Fetching RAWG info for '{}'", rom.getTitulo());
        RawgApiClient.RomInfo info = RawgApiClient.obtenerInfo(rom.getTitulo());
        try {
            if (info != null && info.descripcion != null) {
                rom.setDescripcion(info.descripcion);
                logger.info("Descripción actualizada");

                if (info.imageUrl != null) {
                    String localPath = ImageUtils.downloadAndSaveImage(info.imageUrl, rom.getTitulo(), rom.getId());
                    if (localPath != null) {
                        rom.setImagen(localPath);
                        logger.info("Imagen descargada y guardada: {}", localPath);
                    }
                }

                romService.guardar(rom);
                mostrarDetallesRom(rom.getTitulo());
                MessageUtils.showInfo("ROM data updated from RAWG.io");
            } else {
                MessageUtils.showWarning("No data found on RAWG.io");
            }
        } catch (Exception e) {
            logger.error("Error downloading image from RAWG.io", e);
            MessageUtils.showError("Failed to download image from RAWG.io: " + e.getMessage());
        }
    }

    @FXML
    public void handleSync() {
        logger.info("Sync clicked");
    }

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
            scene.getStylesheets().add(SceneUtils.class.getResource("/styles/romraider.css").toExternalForm());

            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream("/assets/romraider-icon.png")));
            stage.setScene(scene);
            stage.showAndWait();

            logger.info("Preferences window opened");

        } catch (IOException e) {
            logger.error("Error opening Preferences window", e);
        }
    }

    @FXML
    public void handleViewLibrary() {
        logger.info("View: Library clicked");
    }

    @FXML
    public void handleViewStatistics() {
        logger.info("View: Statistics clicked");
    }

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
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/romraider-icon.png")));
            stage.showAndWait();

            cargarPlataformas();

        } catch (IOException e) {
            logger.error("Error opening Platform form", e);
            MessageUtils.showError("Could not open the Platform form.");
        }
    }


    @FXML
    public void handleEditPlatform() {
        Plataforma selected = platformListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MessageUtils.showWarning("Please select a platform to edit.");
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
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/romraider-icon.png")));
            stage.showAndWait();

            cargarPlataformas();
            if (platformListView.getItems().contains(selected)) {
                platformListView.getSelectionModel().select(selected);
            }

        } catch (IOException e) {
            logger.error("Error opening Platform edit form", e);
            MessageUtils.showError("Could not open the Platform form.");
        }
    }


    @FXML
    public void handleDeletePlatform() {
        Plataforma selected = platformListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Deletion");
            alert.setHeaderText("Delete platform '" + selected.getNombre() + "'?");
            alert.setContentText("All associated ROMs will also be deleted. Continue?");

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    romService.eliminarPorPlataforma(selected.getId());
                    plataformaService.eliminar(selected.getId());
                    cargarPlataformas();
                    romListView.getItems().clear();
                    logger.info("Platform deleted: {}", selected.getNombre());
                }
            });
        } else {
            logger.warn("No platform selected to delete");
        }
    }

    @FXML
    public void handleAddRom() {
        if (plataformaSeleccionada == null) {
            MessageUtils.showWarning("Please select a platform first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RomFormView.fxml"));
            Parent root = loader.load();

            RomFormController controller = loader.getController();
            Stage stage = new Stage();
            controller.setPlataforma(plataformaSeleccionada);

            stage.setTitle("Add ROM");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream("/assets/romraider-icon.png")));
            stage.showAndWait();

            cargarRomsPorPlataforma(plataformaSeleccionada); // Refresh list

        } catch (IOException e) {
            logger.error("Error opening ROM form", e);
            MessageUtils.showError("Could not open the ROM form.");
        }
    }

    @FXML
    public void handleEditRom() {
        String selectedTitle = romListView.getSelectionModel().getSelectedItem();
        if (selectedTitle == null) {
            MessageUtils.showInfo("Please select a ROM to edit.");
            return;
        }

        Rom selectedRom = roms.stream()
                .filter(r -> r.getTitulo().equals(selectedTitle))
                .findFirst().orElse(null);

        if (selectedRom == null) {
            MessageUtils.showWarning("Selected ROM not found.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/RomFormView.fxml"));
            Parent root = loader.load();

            RomFormController controller = loader.getController();
            Stage stage = new Stage();
            controller.setRomToEdit(selectedRom);
            controller.setPlataforma(plataformaSeleccionada);

            stage.setTitle("Edit ROM");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.getIcons().add(new Image(SceneUtils.class.getResourceAsStream("/assets/romraider-icon.png")));
            stage.showAndWait();

            cargarRomsPorPlataforma(plataformaSeleccionada);
            mostrarDetallesRom(selectedRom.getTitulo());

        } catch (IOException e) {
            logger.error("Error opening ROM edit form", e);
            MessageUtils.showError("Could not open the ROM form.");
        }
    }


    private Image getDefaultImage() {
        return new Image(getClass().getResourceAsStream("/assets/no-image.png"));
    }


}
