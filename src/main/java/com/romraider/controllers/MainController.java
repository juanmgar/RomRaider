package com.romraider.controllers;

import com.romraider.utils.SceneUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class MainController {

    private boolean offlineMode = false;

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
        if (syncButton != null) {
            syncButton.setDisable(offlineMode);
        }
        if (loginLogoutMenuItem != null) {
            loginLogoutMenuItem.setText(offlineMode ? "Login" : "Logout");
        }
        System.out.println("Main view opened in offline mode: " + offlineMode);
    }

    @FXML
    private MenuBar menuBar;
    @FXML
    private ListView<String> platformListView;
    @FXML
    private Button addRomButton;
    @FXML
    private Button scanFolderButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button importButton;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<String> romListView;
    @FXML
    private ImageView romImage;
    @FXML
    private Label romDescription;
    @FXML
    private Button editButton;
    @FXML
    private Button apiButton;
    @FXML
    private CheckBox favoriteCheckBox;
    @FXML
    private CheckBox playedCheckBox;
    @FXML
    private Button syncButton;
    @FXML
    private Button settingsButton;
    @FXML
    private MenuItem loginLogoutMenuItem;
    @FXML
    private Label userLabel;

    @FXML
    public void initialize() {
        System.out.println("Main view initialized");
        syncButton.setDisable(true); // Default to disabled; enable after login if applicable
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
    public void handleSearch() {
        System.out.println("Search changed: " + searchField.getText());
    }

    @FXML
    public void handleRomSelection() {
        System.out.println("ROM selected");
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
    public void handleLoginLogout() {
        Stage stage = (Stage) menuBar.getScene().getWindow();

        // Establece el estado y lanza la nueva vista
        if (!offlineMode) {
            offlineMode = true;
            System.out.println("User logged out, switching to login view.");
        } else {
            System.out.println("User choosing to log in from offline mode.");
        }

        SceneUtils.switchToLoginView(stage);
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

