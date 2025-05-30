package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.SceneUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.List;

public class MainController {

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
    private Label romDescription;
    @FXML
    private CheckBox favoriteCheckBox;
    @FXML
    private CheckBox playedCheckBox;
    @FXML
    private Button syncButton;
    @FXML
    private MenuItem loginLogoutMenuItem;
    @FXML
    private Label userLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ImageView romImage;

    @FXML
    public void initialize() {
        System.out.println("Main view initialized");
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
        System.out.println("Main view opened in offline mode: " + offlineMode);
    }

    private void cargarPlataformas() {
        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        platformListView.setItems(FXCollections.observableArrayList(plataformas));
    }

    private void cargarRomsPorPlataforma(Plataforma plataforma) {
        roms = romService.obtenerPorPlataforma(plataforma.getId());
        ObservableList<String> romTitulos = FXCollections.observableArrayList(
                roms.stream().map(Rom::getTitulo).toList()
        );
        romListView.setItems(romTitulos);
    }

    private void mostrarDetallesRom(String titulo) {
        Rom rom = roms.stream().filter(r -> r.getTitulo().equals(titulo)).findFirst().orElse(null);
        if (rom != null) {
            romDescription.setText(rom.getDescripcion());
            favoriteCheckBox.setSelected(rom.isFavorito());
            playedCheckBox.setSelected(rom.isJugado());
            // romImage.setImage(...) TODO: Load image if available
        }
    }

    @FXML
    public void handleLoginLogout() {
        Stage stage = (Stage) menuBar.getScene().getWindow();
        offlineMode = !offlineMode;
        SceneUtils.switchToLoginView(stage);
    }

    @FXML
    public void handleSearch() {
        System.out.println("Search changed: " + searchField.getText());
    }

    @FXML
    public void handleAddRom() {
        System.out.println("Add ROM clicked");
    }

    @FXML
    public void handleScanFolder() {
        System.out.println("Scan folder clicked");
    }

    @FXML
    public void handleExport() {
        System.out.println("Export clicked");
    }

    @FXML
    public void handleImport() {
        System.out.println("Import clicked");
    }

    @FXML
    public void handleEditRom() {
        System.out.println("Edit clicked");
    }

    @FXML
    public void handleUpdateFromAPI() {
        System.out.println("Update from API clicked");
    }

    @FXML
    public void handleFavoriteToggle() {
        System.out.println("Favorite toggled: " + favoriteCheckBox.isSelected());
    }

    @FXML
    public void handlePlayedToggle() {
        System.out.println("Played toggled: " + playedCheckBox.isSelected());
    }

    @FXML
    public void handleSync() {
        System.out.println("Sync clicked");
    }

    @FXML
    public void handleSettings() {
        System.out.println("Settings clicked");
    }

    @FXML
    public void handleViewLibrary() {
        System.out.println("View: Library clicked");
    }

    @FXML
    public void handleViewStatistics() {
        System.out.println("View: Statistics clicked");
    }
}
