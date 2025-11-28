package com.romraider.controllers;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import com.romraider.service.PlataformaService;
import com.romraider.service.RomService;
import com.romraider.utils.I18nUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador encargado de generar y mostrar las estadísticas globales
 * de la colección de ROMs y plataformas:
 *
 * - Número total de ROMs
 * - Número total de plataformas
 * - ROMs jugadas y favoritas
 * - Gráfico de barras con ROMs por plataforma
 * - Gráficos circulares para jugadas/no jugadas y favoritas/no favoritas
 *
 * Esta vista es de solo lectura.
 */
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

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

    /**
     * Inicializa la vista de estadísticas cargando datos desde la base local
     * y rellenando los gráficos.
     */
    @FXML
    public void initialize() {
        logger.info("Inicializando estadísticas...");

        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        List<Rom> roms = romService.obtenerTodas();

        // Contadores globales

        long playedCount = roms.stream().filter(Rom::isJugado).count();
        long favoriteCount = roms.stream().filter(Rom::isFavorito).count();

        // Labels
        totalRomsLabel.setText(String.format(
                I18nUtils.get("statistics.totalRoms"), roms.size()
        ));

        totalPlatformsLabel.setText(String.format(
                I18nUtils.get("statistics.totalPlatforms"), plataformas.size()
        ));

        romsPlayedLabel.setText(String.format(
                I18nUtils.get("statistics.romsPlayed"), playedCount
        ));

        romsFavoritedLabel.setText(String.format(
                I18nUtils.get("statistics.romsFavorited"), favoriteCount
        ));

        logger.info("ROMs totales: {}, jugadas: {}, favoritas: {}",
                roms.size(), playedCount, favoriteCount);


        /*
         *
         *  GRÁFICO DE BARRAS
         *  ROMs agrupadas por plataforma
         *
         */
        Map<String, Long> romsPorPlataforma =
                plataformas.stream()
                        .collect(Collectors.toMap(
                                Plataforma::getNombre,
                                p -> roms.stream()
                                        .filter(r -> r.getPlataforma().getNombre().equals(p.getNombre()))
                                        .count()
                        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(I18nUtils.get("statistics.romsPerPlatform"));

        romsPorPlataforma.forEach((platform, count) -> {
            logger.debug("Plataforma '{}' tiene {} ROMs", platform, count);
            series.getData().add(new XYChart.Data<>(platform, count));
        });

        romsPerPlatformChart.getData().add(series);

        /*
         *
         *  PIE CHART: Jugadas / No jugadas
         *
         */
        playedPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.played"), playedCount
        ));
        playedPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.notPlayed"), roms.size() - playedCount
        ));


        /*
         *
         *  PIE CHART: Favoritas / No favoritas
         *
         */
        favoritesPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.favorite"), favoriteCount
        ));
        favoritesPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.notFavorite"), roms.size() - favoriteCount
        ));

        logger.info("Estadísticas cargadas correctamente.");
    }

    /**
     * Cierra la ventana de estadísticas.
     */
    @FXML
    private void handleClose() {
        logger.info("Cerrando ventana de estadísticas");
        totalRomsLabel.getScene().getWindow().hide();
    }
}
