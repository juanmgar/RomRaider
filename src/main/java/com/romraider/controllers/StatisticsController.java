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
 * de la colección de ROMs y plataformas.
 *
 * <p>La vista muestra:</p>
 * <ul>
 *     <li>Número total de ROMs</li>
 *     <li>Número total de plataformas</li>
 *     <li>ROMs jugadas y marcadas como favoritas</li>
 *     <li>Gráfico de barras con ROMs por plataforma</li>
 *     <li>Gráficos circulares de jugadas/no jugadas y favoritas/no favoritas</li>
 * </ul>
 *
 * <p>Esta vista es de solo lectura y no modifica datos.</p>
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

    /** Servicio para acceso a plataformas. */
    private final PlataformaService plataformaService = new PlataformaService();

    /** Servicio para acceso a ROMs. */
    private final RomService romService = new RomService();

    /**
     * Inicializa la vista de estadísticas una vez cargado el FXML.
     *
     * <p>Realiza:</p>
     * <ul>
     *     <li>Carga de datos desde la base local</li>
     *     <li>Población de labels informativos</li>
     *     <li>Construcción del gráfico de barras</li>
     *     <li>Construcción de los gráficos circulares</li>
     * </ul>
     *
     * <p>Se ejecuta automáticamente por JavaFX.</p>
     */
    @FXML
    public void initialize() {
        logger.info("Inicializando estadísticas...");

        List<Plataforma> plataformas = plataformaService.obtenerTodas();
        List<Rom> roms = romService.obtenerTodas();

        long playedCount = roms.stream().filter(Rom::isJugado).count();
        long favoriteCount = roms.stream().filter(Rom::isFavorito).count();

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
         * GRÁFICO DE BARRAS: ROMs agrupadas por plataforma
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
         * PIE CHART: Jugadas / No jugadas
         */
        playedPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.played"), playedCount
        ));
        playedPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.notPlayed"), (double) roms.size() - playedCount
        ));

        /*
         * PIE CHART: Favoritas / No favoritas
         */
        favoritesPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.favorite"), favoriteCount
        ));
        favoritesPieChart.getData().add(new PieChart.Data(
                I18nUtils.get("statistics.notFavorite"), (double) roms.size() - favoriteCount
        ));

        logger.info("Estadísticas cargadas correctamente.");
    }

    /**
     * Cierra la ventana emergente de estadísticas.
     *
     * <p>Este método es invocado desde el botón “Cerrar”.</p>
     */
    @FXML
    private void handleClose() {
        logger.info("Cerrando ventana de estadísticas");
        totalRomsLabel.getScene().getWindow().hide();
    }
}
