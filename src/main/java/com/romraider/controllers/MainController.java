package com.romraider.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class MainController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> platformListView;

    @FXML
    private ListView<String> romListView;

    @FXML
    private Button addRomButton;

    @FXML
    private Button logOutButton;

    // Datos simulados de ROMs para pruebas iniciales
    private final ObservableList<String> allRoms = FXCollections.observableArrayList(
            "Super Mario Bros. - NES · 1985 · Platformer",
            "Final Fantasy III - SNES · 1994 · RPG",
            "Sonic the Hedgehog - GEN · 1991 · Platformer",
            "Tetris - GB · 1989 · Puzzle"
    );

    @FXML
    public void initialize() {
        romListView.setItems(FXCollections.observableArrayList(allRoms));

        platformListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            filterRoms();
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterRoms();
        });

        addRomButton.setOnAction(event -> onAddRom());

        logOutButton.setOnAction(event -> onLogout());
    }

    private void filterRoms() {
        String selectedPlatform = platformListView.getSelectionModel().getSelectedItem();
        String searchQuery = searchField.getText().toLowerCase();

        ObservableList<String> filtered = allRoms.filtered(rom ->
                (selectedPlatform == null || selectedPlatform.equals("All") || rom.toLowerCase().contains(selectedPlatform.toLowerCase()))
                        && rom.toLowerCase().contains(searchQuery)
        );

        romListView.setItems(filtered);
    }

    private void onAddRom() {
        System.out.println("Add ROM clicked!");
    }

    private void onLogout() {
        System.out.println("Logging out...");
    }
}
