package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsController {

    @FXML
    private Label totalRomsLabel;

    @FXML
    private Label totalPlatformsLabel;

    @FXML
    private Label romsPlayedLabel;

    @FXML
    private Label romsFavoritedLabel;

    @FXML
    private BarChart<String, Number> romsPerPlatformChart;

    @FXML
    private PieChart playedPieChart;

    @FXML
    private PieChart favoritesPieChart;

    private final PlataformaService plataformaService = new PlataformaService();
    private final RomService romService = new RomService();

    @FXML
    public void initialize() {
        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        List<Rom> roms = romService.obtenerTodas();

        totalRomsLabel.setText("Total ROMs: " + roms.size());
        totalPlatformsLabel.setText("Total Platforms: " + plataformas.size());

        long playedCount = roms.stream().filter(Rom::isJugado).count();
        long favoriteCount = roms.stream().filter(Rom::isFavorito).count();

        romsPlayedLabel.setText("ROMs Played: " + playedCount);
        romsFavoritedLabel.setText("ROMs Favorited: " + favoriteCount);

        // Bar chart: ROMs por plataforma
        Map<String, Long> romsPorPlataforma = plataformas.stream()
                .collect(Collectors.toMap(
                        Plataforma::getNombre,
                        p -> roms.stream().filter(r -> r.getPlataforma().getNombre().equals(p.getNombre())).count()
                ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        romsPorPlataforma.forEach((platform, count) ->
                series.getData().add(new XYChart.Data<>(platform, count)));
        romsPerPlatformChart.getData().add(series);

        // Pie chart: Jugado vs No jugado
        playedPieChart.getData().add(new PieChart.Data("Played", playedCount));
        playedPieChart.getData().add(new PieChart.Data("Not Played", roms.size() - playedCount));

        // Pie chart: Favorito vs No favorito
        favoritesPieChart.getData().add(new PieChart.Data("Favorite", favoriteCount));
        favoritesPieChart.getData().add(new PieChart.Data("Not Favorite", roms.size() - favoriteCount));
    }

    @FXML
    private void handleClose() {
        Label label = totalRomsLabel;
        label.getScene().getWindow().hide();
    }

}
