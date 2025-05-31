package com.romraider.controllers;

import com.romraider.api.RawgApiClient;
import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.MessageUtils;
import com.romraider.utils.SceneUtils;
import com.romraider.utils.XMLUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.romraider.api.RawgApiClient.descargarImagen;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final PlataformaService plataformaService = new PlataformaService();
    private final RomService romService = new RomService();
    private boolean offlineMode = false;

    private Plataforma plataformaSeleccionada;
    private List<Rom> roms;

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
        logger.info("Main view initialized");
        syncButton.setDisable(true);
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
        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        platformListView.setItems(FXCollections.observableArrayList(plataformas));
        logger.debug("Loaded {} plataformas", plataformas.size());
    }

    private void cargarRomsPorPlataforma(Plataforma plataforma) {
        roms = romService.obtenerPorPlataforma(plataforma.getId());
        ObservableList<String> romTitulos = FXCollections.observableArrayList(
                roms.stream().map(Rom::getTitulo).toList()
        );
        romListView.setItems(romTitulos);
        logger.debug("Loaded {} ROMs for platform {}", roms.size(), plataforma.getNombre());
    }

    private void mostrarDetallesRom(String titulo) {
        Rom rom = roms.stream().filter(r -> r.getTitulo().equals(titulo)).findFirst().orElse(null);
        if (rom != null) {
            romDescription.setText(rom.getDescripcion() != null ? rom.getDescripcion() : "(No description)");
            favoriteCheckBox.setSelected(rom.isFavorito());
            playedCheckBox.setSelected(rom.isJugado());
            romImage.setImage(
                    rom.getImagen() != null && !rom.getImagen().isBlank()
                            ? new Image("file:" + rom.getImagen(), true)
                            : getDefaultImage()
            );

            logger.debug("Details shown for ROM: {}", rom.getTitulo());
        }
    }

    @FXML
    public void handleLoginLogout() {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        offlineMode = !offlineMode;
        logger.info("Toggling login/logout. Offline mode now: {}", offlineMode);
        SceneUtils.switchToLoginView(stage);
    }

    @FXML
    public void handleSearch() {
        logger.info("Search changed: {}", searchField.getText());
    }

    @FXML
    public void handleScanFolder() {
        logger.info("Scan folder clicked");
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
                        List<Plataforma> plataformasImportadas = XMLUtils.importarDesdeXml(selectedFile);
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

        if (info != null && info.descripcion != null) {
            rom.setDescripcion(info.descripcion);
            logger.info("Descripción actualizada");

            if (info.imageUrl != null) {
                String localPath = descargarImagen(info.imageUrl, rom.getTitulo(), rom.getId());
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
