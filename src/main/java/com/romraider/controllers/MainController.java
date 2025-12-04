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
import java.util.*;
import java.util.stream.Collectors;

import static com.romraider.db.DataInitializer.insertOrUpdateDefaultPlatforms;

/**
 * Controlador principal de la aplicación ROM Raider.
 *
 * <p>Gestiona toda la lógica de la vista principal, incluyendo:</p>
 * <ul>
 *     <li>Listado de plataformas y ROMs</li>
 *     <li>Búsqueda dinámica</li>
 *     <li>Visualización de detalles de una ROM</li>
 *     <li>Operaciones CRUD sobre plataformas y ROMs</li>
 *     <li>Escaneo automático desde carpetas del sistema</li>
 *     <li>Importación/exportación en XML</li>
 *     <li>Actualización de datos mediante la API de RAWG.io</li>
 *     <li>Sincronización completa con Supabase</li>
 * </ul>
 *
 * <p>La clase implementa un controlador complejo que actúa como
 * centro de operaciones del sistema, integrando persistencia,
 * red, API externa, interfaz gráfica y sincronización con la nube.</p>
 *
 * <p>Es la clase de mayor responsabilidad en la aplicación,
 * integrando múltiples servicios e interacciones con la UI.</p>
 */
public class MainController {


    /**
     * Logger principal para trazabilidad del controlador.
     */
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    /**
     * Servicio encargado de gestionar operaciones CRUD de plataformas.
     */
    private final PlataformaService plataformaService = new PlataformaService();

    /**
     * Servicio encargado de gestionar operaciones CRUD de ROMs.
     */
    private final RomService romService = new RomService();

    /**
     * Servicio para actualizar datos de ROM usando la API de RAWG.io.
     */
    private final RawgRomUpdateService rawgRomUpdateService = new RawgRomUpdateService(romService);

    /**
     * Plataforma actualmente seleccionada en la UI.
     */
    private Plataforma plataformaSeleccionada;

    /**
     * Lista completa de ROMs cargadas para la plataforma seleccionada.
     */
    private List<Rom> roms;

    /**
     * Lista observable con los títulos de ROMs mostrados en pantalla.
     */
    private ObservableList<String> romTitulos = FXCollections.observableArrayList();

    /**
     * Lista filtrada para permitir la búsqueda dinámica de ROMs.
     */
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

    /**
     * Inicializa la vista principal tras cargar el FXML.
     *
     * <p>Responsabilidades:</p>
     * <ul>
     *     <li>Detectar conexión a Internet</li>
     *     <li>Configurar UI según si el usuario está online/offline</li>
     *     <li>Cargar la lista de plataformas</li>
     *     <li>Mostrar fecha de última sincronización</li>
     *     <li>Configurar listeners para selección de plataforma y ROM</li>
     * </ul>
     *
     * <p>Este método es el punto de arranque del controlador,
     * preparando todos los elementos interactivos de la vista.</p>
     */
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

    /**
     * Carga la lista completa de plataformas desde la base de datos
     * y la muestra en el ListView de la interfaz.
     *
     * <p>Incluye una plataforma virtual denominada "Todas", con id -1,
     * que permite mostrar todas las ROMs independientemente de su plataforma.</p>
     *
     * <p>Acciones realizadas:</p>
     * <ul>
     *     <li>Obtiene todas las plataformas almacenadas en BD</li>
     *     <li>Inserta una opción "Todas" en la primera posición</li>
     *     <li>Rellena el ListView con un ObservableList</li>
     *     <li>Selecciona automáticamente la primera plataforma</li>
     *     <li>Carga las ROM asociadas a dicha plataforma</li>
     * </ul>
     */
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

    /**
     * Carga todas las ROMs asociadas a una plataforma concreta
     * y actualiza la lista visual de títulos en pantalla.
     *
     * <p>Comportamiento:</p>
     * <ul>
     *     <li>Si la plataforma tiene id = -1 (caso "Todas"),
     *         se cargan todas las ROMs del sistema.</li>
     *     <li>Ordena alfabéticamente los títulos</li>
     *     <li>Actualiza la lista observable utilizada por el ListView</li>
     *     <li>Configura o refresca la FilteredList asociada a la búsqueda</li>
     * </ul>
     *
     * @param plataforma plataforma seleccionada en la UI
     */
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

    /**
     * Muestra en pantalla los detalles completos de una ROM seleccionada:
     * descripción, plataforma, flags (favorita/jugada) e imagen.
     *
     * <p>Flujo:</p>
     * <ul>
     *     <li>Busca la ROM en memoria según su título</li>
     *     <li>Actualiza descripción y plataforma en la UI</li>
     *     <li>Marca los checkboxes según estado de la ROM</li>
     *     <li>Carga imagen desde disco o muestra una imagen por defecto</li>
     * </ul>
     *
     * <p>Si no se encuentra la ROM o el archivo de imagen no existe,
     * la imagen se sustituye por un recurso genérico.</p>
     *
     * @param titulo título de la ROM seleccionada en el ListView
     */
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

    /**
     * Maneja el comportamiento del menú Login/Logout.
     *
     * <p>Si el usuario está logueado:</p>
     * <ul>
     *     <li>Cierra sesión en Supabase</li>
     *     <li>Elimina el token persistido en disco</li>
     *     <li>Regresa a la pantalla de login</li>
     * </ul>
     *
     * <p>Si no está logueado, simplemente navega al login.</p>
     */

    @FXML
    public void handleLoginLogout() {
        logger.info("Logging out and returning to login screen");

        SupabaseAuthService.logout();
        SessionManager.clearSession();

        Stage stage = (Stage) menuBar.getScene().getWindow();
        SceneUtils.switchToLoginView(stage);
    }

    /**
     * Ejecuta la lógica de búsqueda dinámica de ROMs.
     *
     * <p>La búsqueda filtra la lista observable de títulos para
     * mostrar solo aquellos que contienen el texto introducido.</p>
     *
     * <p>El filtrado es insensible a mayúsculas/minúsculas.</p>
     */
    @FXML
    public void handleSearch() {

        String filtro = searchField.getText().toLowerCase().trim();
        logger.info("Search changed: {}", filtro);

        if (romFiltradas != null) {
            romFiltradas.setPredicate(titulo -> titulo.toLowerCase().contains(filtro));
        }
    }

    /**
     * Maneja el proceso de escaneo de ROMs desde una carpeta seleccionada por el usuario.
     *
     * <p>Flujo completo:</p>
     * <ol>
     *     <li>Abre un diálogo para elegir un directorio</li>
     *     <li>Valida que exista la configuración de carpeta destino</li>
     *     <li>Muestra un overlay con spinner durante el proceso</li>
     *     <li>Ejecuta el escaneo en un hilo en segundo plano mediante {@link Task}</li>
     *     <li>Al finalizar:
     *         <ul>
     *             <li>Oculta overlay</li>
     *             <li>Recarga plataformas</li>
     *             <li>Muestra un resumen de ROMs importadas</li>
     *         </ul>
     *     </li>
     * </ol>
     */

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

    /**
     * Actualiza dinámicamente el texto del menú Login/Logout
     * y el label de usuario según el estado de autenticación.
     *
     * <p>Si hay usuario logueado:</p>
     * <ul>
     *     <li>Muestra su email</li>
     *     <li>El menú indica "Logout"</li>
     * </ul>
     *
     * <p>Si no hay sesión activa:</p>
     * <ul>
     *     <li>La etiqueta muestra "Offline"</li>
     *     <li>El menú indica "Login"</li>
     * </ul>
     */
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

    /**
     * Procesa un archivo ROM individual detectado durante el escaneo de carpetas.
     *
     * <p>Acciones realizadas:</p>
     * <ul>
     *     <li>Detecta la extensión del archivo y determina su plataforma destino</li>
     *     <li>Ignora archivos no soportados o ya existentes en la BD</li>
     *     <li>Crea el directorio final según la plataforma si no existe</li>
     *     <li>Mueve el archivo ROM desde el directorio escaneado a la carpeta principal del usuario</li>
     *     <li>Crea una nueva entidad {@code Rom} y la persiste en la base de datos</li>
     *     <li>Opcionalmente actualiza metadatos desde RAWG.io si está activo</li>
     * </ul>
     *
     * <p>Este método encapsula toda la lógica de tratamiento de una ROM nueva.</p>
     *
     * @param path         ruta del archivo ROM encontrado durante el escaneo
     * @param baseFolder   carpeta raíz de destino donde se almacenarán las ROMs
     * @param extensionMap mapa {extensión → plataforma} para asignación rápida
     */
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

    /**
     * Exporta toda la colección a un archivo XML.
     *
     * <p>Acciones:</p>
     * <ul>
     *     <li>Muestra diálogo para seleccionar nombre y ubicación del archivo .xml</li>
     *     <li>Obtiene todas las plataformas incluyendo sus ROMs</li>
     *     <li>Llama a {@link XMLUtils#exportarAxml(List, File)}</li>
     *     <li>Muestra mensaje de éxito o error</li>
     * </ul>
     *
     * <p>El archivo generado contiene:
     *     <b>plataformas, roms, metadatos, imágenes (solo rutas), flags, descripciones.</b>
     * </p>
     */
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

    /**
     * Importa una colección desde un archivo XML externo.
     *
     * <p>Procedimiento:</p>
     * <ol>
     *     <li>Permite seleccionar un archivo XML desde un diálogo</li>
     *     <li>Pide confirmación al usuario antes de sobrescribir datos</li>
     *     <li>Muestra overlay con spinner</li>
     *     <li>Crea y ejecuta un {@link Task} que:
     *         <ul>
     *             <li>Lee el XML con {@link XMLUtils#importarDesdeXml(File)}</li>
     *             <li>Elimina las plataformas existentes</li>
     *             <li>Inserta las plataformas importadas con sus ROMs</li>
     *         </ul>
     *     </li>
     *     <li>Al finalizar, recarga plataformas y muestra mensaje de éxito</li>
     * </ol>
     *
     * <p>Este proceso sustituye completamente la base local.</p>
     */
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
     * Actualiza la ROM seleccionada consultando la API de RAWG.io.
     *
     * <p>Acciones realizadas:</p>
     * <ul>
     *     <li>Obtiene descripción e imagen actualizadas</li>
     *     <li>Actualiza la base de datos local</li>
     *     <li>Refresca la UI mostrando los nuevos datos</li>
     *     <li>Muestra mensajes correspondientes según el resultado:
     *         <ul>
     *             <li>UPDATED → actualización exitosa</li>
     *             <li>NOT_FOUND → RAWG no encontró coincidencias</li>
     *             <li>ERROR → problema en la petición o procesamiento</li>
     *         </ul>
     *     </li>
     * </ul>
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
     * Actualiza todas las ROMs presentes en la base de datos usando RAWG.io.
     *
     * <p>Proceso completo:</p>
     * <ol>
     *     <li>Solicita confirmación al usuario</li>
     *     <li>Obtiene todas las ROMs existentes</li>
     *     <li>Muestra overlay con spinner</li>
     *     <li>Ejecuta en un Task:
     *         <ul>
     *             <li>Intenta actualizar cada ROM individualmente</li>
     *             <li>Cuenta cuántas se actualizaron o no fueron encontradas</li>
     *         </ul>
     *     </li>
     *     <li>Al finalizar, muestra un resumen detallado</li>
     *     <li>Recarga plataformas</li>
     * </ol>
     *
     * <p>Es una operación intensiva diseñada para ejecutarse en un hilo aparte.</p>
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
     * Realiza la sincronización completa con Supabase.
     *
     * <p>Durante la sincronización:</p>
     * <ul>
     *     <li>Se muestra overlay con spinner</li>
     *     <li>Se deshabilita el botón Sync</li>
     *     <li>Ejecuta en un Task la llamada a {@link SupabaseSyncService#syncWithSupabase()}</li>
     *     <li>Actualiza la fecha de último sync</li>
     *     <li>Reproduce un sonido al finalizar</li>
     * </ul>
     *
     * <p>En caso de error, se notifica al usuario y se reactivan los controles.</p>
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
     * Muestra la imagen de la ROM en una ventana aparte a mayor tamaño.
     */
    @FXML
    public void handleShowLargeImage() {
        Image image = romImage.getImage();
        if (image == null) {
            return;
        }

        Stage owner = (Stage) menuBar.getScene().getWindow();

        Stage stage = new Stage();
        stage.setTitle(romListView.getSelectionModel().getSelectedItem());
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);

        StackPane root = new StackPane(imageView);
        root.setStyle("-fx-padding: 10; -fx-background-color: #000000;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
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
     * Abre el formulario para crear una nueva plataforma.
     *
     * <p>Flujo:</p>
     * <ol>
     *     <li>Abre un diálogo modal usando {@link DialogUtils}</li>
     *     <li>Permite introducir nombre, extensión y carpeta</li>
     *     <li>Al cerrarse el formulario, recarga la lista de plataformas</li>
     *     <li>Mantiene seleccionada la plataforma actual, si procede</li>
     * </ol>
     *
     * <p>El formulario no se cierra automáticamente si hay errores.</p>
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
     * Permite editar la plataforma actualmente seleccionada.
     *
     * <p>Reglas:</p>
     * <ul>
     *     <li>Debe seleccionarse una plataforma concreta</li>
     *     <li>No se permite editar la opción "Todas"</li>
     * </ul>
     *
     * <p>Tras completar edición:</p>
     * <ul>
     *     <li>Recarga la lista de plataformas</li>
     *     <li>Restaura la selección previa si la plataforma sigue existiendo</li>
     * </ul>
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
     * Elimina la plataforma seleccionada, junto con todas sus ROMs asociadas.
     *
     * <p>Flujo completo:</p>
     * <ol>
     *     <li>Comprueba selección válida (no "Todas")</li>
     *     <li>Muestra diálogo de confirmación</li>
     *     <li>Elimina imágenes asociadas mediante {@link ImageUtils}</li>
     *     <li>Elimina ROMs y luego la plataforma</li>
     *     <li>Recarga lista de plataformas</li>
     *     <li>Vacía lista de ROMs y restaura selección a "Todas"</li>
     * </ol>
     *
     * <p>Se informa de errores con mensajes amigables.</p>
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
     * Abre el formulario para crear una nueva ROM asociada a una plataforma.
     *
     * <p>Reglas:</p>
     * <ul>
     *     <li>Debe haber una plataforma seleccionada</li>
     *     <li>El formulario permite introducir ruta, imagen y datos descriptivos</li>
     * </ul>
     *
     * <p>Tras cerrar el formulario, se recarga la lista de ROMs de la plataforma actual.</p>
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
     * Abre el formulario para editar la ROM seleccionada.
     *
     * <p>Flujo:</p>
     * <ol>
     *     <li>Valida selección</li>
     *     <li>Encuentra la ROM correspondiente dentro de la lista cargada</li>
     *     <li>Abre el formulario configurado en modo edición</li>
     *     <li>Al finalizar, recarga lista de ROMs y refresca los detalles mostrados</li>
     * </ol>
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
     * Elimina la ROM actualmente seleccionada de la base de datos.
     *
     * <p>Acciones asociadas:</p>
     * <ul>
     *     <li>Confirmación del usuario</li>
     *     <li>Eliminación en BD</li>
     *     <li>Borrado de imagen asociada si existe</li>
     *     <li>Recarga de ROMs de la plataforma actual</li>
     *     <li>Reseteo de panel de detalles</li>
     * </ul>
     *
     * <p>Si falla, muestra mensaje con el error encontrado.</p>
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
     * Abre la ventana de créditos del proyecto.
     *
     * <p>Mostrado en un diálogo modal, incluye enlaces a recursos
     * gráficos y menciones a autores.</p>
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
     * Abre el manual de ayuda (HTML incrustado dentro del JAR).
     *
     * <p>La vista utiliza un {@link javafx.scene.web.WebView}
     * para renderizar el manual almacenado en {@code /manual/manual.html}.</p>
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

    /**
     * Abre la carpeta donde se encuentra almacenada la ROM seleccionada.
     *
     * <p>Validaciones:</p>
     * <ul>
     *     <li>Debe haber una ROM seleccionada</li>
     *     <li>La ROM debe tener ruta válida</li>
     *     <li>La carpeta debe existir</li>
     *     <li>El sistema debe soportar Desktop.open()</li>
     * </ul>
     *
     * <p>Si todo es correcto, abre la carpeta en el explorador de archivos del sistema.</p>
     */
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

    /**
     * Abre un diálogo de selección de carpeta y valida que exista.
     *
     * @return el directorio válido seleccionado, o {@code null} si el usuario cancela
     */
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

    /**
     * Construye el mensaje final mostrado tras un escaneo de ROMs.
     *
     * <p>Incluye:</p>
     * <ul>
     *     <li>Total de ROMs añadidas</li>
     *     <li>Número de plataformas afectadas</li>
     *     <li>Listado con nombres de cada plataforma</li>
     * </ul>
     *
     * <p>El formato está localizado mediante I18nUtils.</p>
     */
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

    /**
     * Crea un {@link Task} encargado de escanear una carpeta en segundo plano.
     *
     * <p>Responsabilidades del task:</p>
     * <ul>
     *     <li>Recorrer recursivamente todos los archivos dentro del directorio</li>
     *     <li>Identificar ROMs válidas por extensión</li>
     *     <li>Llamar a {@code procesarArchivo()} para cada ROM detectada</li>
     *     <li>Generar un resumen final con:
     *         <ul>
     *             <li>Número de ROMs añadidas</li>
     *             <li>Plataformas afectadas</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <p>Este método no ejecuta el escaneo: solo construye la tarea para
     * ser ejecutada en un hilo independiente.</p>
     *
     * @param selectedDir carpeta seleccionada por el usuario
     * @param baseFolder  carpeta base donde se almacenan las ROMs procesadas
     * @return Task con el proceso completo de escaneo
     */
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

    /**
     * Procesa un archivo individual dentro del escaneo masivo.
     *
     * <p>Comportamiento:</p>
     * <ul>
     *     <li>Extrae extensión y determina la plataforma correspondiente</li>
     *     <li>Ignora archivos sin extensión compatible</li>
     *     <li>Si la ROM ya existe en BD, se ignora</li>
     *     <li>Delegación interna a {@link #processRomFile(Path, String, Map)}</li>
     *     <li>Actualiza contadores internos del task (ROMs añadidas)</li>
     * </ul>
     *
     * @param path                 archivo detectado durante el escaneo
     * @param extensionMap         mapa {extensión → plataforma}
     * @param baseFolder           carpeta raíz de destino
     * @param romsAdded            contador acumulado de ROMs agregadas
     * @param plataformasAfectadas conjunto de plataformas modificadas
     */
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

    /**
     * Abre un diálogo para seleccionar un archivo XML válido.
     *
     * @return el archivo seleccionado, o {@code null} si se cancela
     */
    private File seleccionarArchivoImportacion() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(I18nUtils.get("main.import.dialogTitle"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(I18nUtils.get("main.import.filter.xml"), "*.xml")
        );
        return fileChooser.showOpenDialog(menuBar.getScene().getWindow());
    }

    /**
     * Muestra un diálogo de confirmación previa a la importación de XML.
     *
     * @return true si el usuario confirma la operación, false en caso contrario
     */
    private boolean mostrarDialogoConfirmacion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18nUtils.get("main.import.confirm.title"));
        alert.setHeaderText(I18nUtils.get("main.import.confirm.header"));
        alert.setContentText(I18nUtils.get("main.import.confirm.content"));

        return alert.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    /**
     * Construye la tarea en segundo plano encargada de realizar el proceso completo
     * de importación desde un archivo XML.
     *
     * <p>Responsabilidades del Task:</p>
     * <ul>
     *     <li>Leer el archivo XML usando XMLUtils</li>
     *     <li>Eliminar todas las plataformas actuales</li>
     *     <li>Normalizar listas de ROMs importadas para evitar problemas con JPA</li>
     *     <li>Persistir plataformas y ROMs importadas</li>
     * </ul>
     *
     * @param selectedFile archivo XML elegido por el usuario
     * @return Task listo para ejecutarse en un Thread
     */
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

    /**
     * Se ejecuta cuando la importación XML finaliza correctamente.
     *
     * <p>Acciones:</p>
     * <ul>
     *     <li>Oculta overlay</li>
     *     <li>Recarga plataformas</li>
     *     <li>Vacía la lista de ROMs</li>
     *     <li>Muestra mensaje de éxito</li>
     * </ul>
     */
    private void postImportSuccess(File selectedFile, StackPane overlay) {
        OverlayUtils.hideLoading((Pane) menuBar.getScene().getRoot(), overlay);

        romListView.setItems(FXCollections.observableArrayList());
        cargarPlataformas();

        MessageUtils.showInfo(I18nUtils.get("main.import.success"));
        logger.info("Imported collection from: {}", selectedFile.getAbsolutePath());
    }

    /**
     * Maneja un fallo en la importación XML.
     *
     * <p>Acciones:</p>
     * <ul>
     *     <li>Oculta overlay</li>
     *     <li>Registra el error completo en logs</li>
     *     <li>Muestra mensaje de error al usuario</li>
     * </ul>
     */
    private void postImportFailure(Throwable ex, StackPane overlay) {
        OverlayUtils.hideLoading((Pane) menuBar.getScene().getRoot(), overlay);

        logger.error("Import failed", ex);
        MessageUtils.showError(
                I18nUtils.get("main.import.failedPrefix") + ex.getMessage()
        );
    }

    /**
     * Devuelve la imagen por defecto usada cuando una ROM no tiene portada asignada.
     *
     * @return imagen "no-image.png" cargada desde los recursos del proyecto
     */
    private Image getDefaultImage() {
        return new Image(getClass().getResourceAsStream("/assets/no-image.png"));
    }
}
