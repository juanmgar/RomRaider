package com.romraider.db;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Inicializador de datos por defecto para la aplicación.
 *
 * <p>Esta clase inserta las plataformas y ROMs iniciales en la base de datos
 * cuando esta se encuentra vacía. Está diseñada para poblar el sistema con
 * datos básicos que faciliten las pruebas o el uso inicial de la aplicación.</p>
 *
 * <p>Incluye:</p>
 * <ul>
 *     <li>Listado amplio de plataformas retro y modernas</li>
 *     <li>ROMs de ejemplo asociadas a cada plataforma</li>
 *     <li>Métodos para insertar datos iniciales y evitar duplicados</li>
 * </ul>
 */
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    /** Constantes de nombre para plataformas utilizadas en los ROMs por defecto. */
    private static final String GAME_BOY = "Game Boy";
    private static final String GAME_BOY_ADVANCE = "Game Boy Advance";
    private static final String GENESIS = "Genesis";
    private static final String NINTENDO_64 = "Nintendo 64";

    /** Directorios ficticios donde se ubicarían los ROMs de ejemplo. */
    private static final String NES_DIR = "/roms/nes/";
    private static final String SNES_DIR = "/roms/snes/";
    private static final String GB_DIR = "/roms/gb/";
    private static final String GBA_DIR = "/roms/gba/";
    private static final String GENESIS_DIR = "/roms/genesis/";
    private static final String N64_DIR = "/roms/n64/";

    /** Extensiones típicas según plataforma retro dada. */
    private static final String EXT_NES = ".nes";
    private static final String EXT_SFC = ".sfc";
    private static final String EXT_GB = ".gb";
    private static final String EXT_GBA = ".gba";
    private static final String EXT_GEN = ".gen";
    private static final String EXT_N64 = ".n64";

    /**
     * Conjunto de plataformas predefinidas.
     *
     * Cada entrada contiene:
     * <pre>
     *   [ nombre, extension, carpeta ]
     * </pre>
     */
    private static final String[][] DEFAULT_PLATFORMS = {
            // Nintendo
            {"NES", ".nes", "nes"},
            {"SNES", ".smc", "snes"},
            {NINTENDO_64, ".n64", "n64"},
            {"GameCube", ".gcm", "gamecube"},
            {"Wii", ".wbfs", "wii"},
            {GAME_BOY, ".gb", "gb"},
            {"Game Boy Color", ".gbc", "gbc"},
            {GAME_BOY_ADVANCE, ".gba", "gba"},
            {"Nintendo DS", ".nds", "nds"},
            {"Nintendo 3DS", ".3ds", "3ds"},
            {"Nintendo Switch", ".nsp", "switch"},

            // Sega
            {"Master System", ".sms", "sms"},
            {GENESIS, ".md", "genesis"},
            {"Sega CD", ".scd", "segacd"},
            {"Sega 32X", ".32x", "32x"},
            {"Sega Saturn", ".sat", "saturn"},
            {"Dreamcast", ".cdi", "dreamcast"},

            // Sony
            {"PlayStation", ".psx", "psx"},
            {"PlayStation 2", ".ps2", "ps2"},
            {"PSP", ".iso", "psp"},

            // Atari / Retro
            {"Atari 2600", ".a26", "atari2600"},
            {"Atari 7800", ".a78", "atari7800"},
            {"Neo Geo", ".neo", "neogeo"},
            {"MAME", ".zip", "mame"},

            // PC
            {"MS-DOS", ".exe", "msdos"},

            // Commodore
            {"Commodore 64", ".d64", "c64"},

            // Amiga
            {"Amiga", ".adf", "amiga"},

            // Microordenadores
            {"Amstrad CPC", ".dsk", "cpc"},
            {"ZX Spectrum", ".tzx", "zxspectrum"},

            // NEC
            {"TurboGrafx-16", ".pce", "pcengine"},

            // Bandai
            {"WonderSwan", ".ws", "wonderswan"},
            {"WonderSwan Color", ".wsc", "wsc"},

            // SNK Portable
            {"Neo Geo Pocket", ".ngc", "ngpc"},

            // 1990s Classics
            {"3DO", ".iso3do", "3do"},

            // Old consoles
            {"ColecoVision", ".col", "coleco"},
            {"Intellivision", ".int", "intv"}
    };

    /**
     * Conjunto de ROMs de ejemplo para poblar la base de datos en la primera ejecución.
     *
     * Cada entrada contiene:
     * <pre>
     *   [titulo, descripcion, imagen, ruta_rom, favorito, jugado, nombre_plataforma]
     * </pre>
     */
    private static final Object[][] DEFAULT_ROMS = {
            // NES
            {"Super Mario Bros.", "Clásico de plataformas de Nintendo.", null, NES_DIR + "super_mario_bros" + EXT_NES, true, true, "NES"},
            {"The Legend of Zelda", "Aventura de exploración y acción.", null, NES_DIR + "zelda" + EXT_NES, true, false, "NES"},
            {"Metroid", "Exploración alienígena con acción y plataformas.", null, NES_DIR + "metroid" + EXT_NES, false, true, "NES"},
            {"Castlevania", "Acción y horror gótico en 8 bits.", null, NES_DIR + "castlevania" + EXT_NES, false, false, "NES"},

            // SNES
            {"Super Mario World", "Colorido y amplio juego de plataformas.", null, SNES_DIR + "super_mario_world" + EXT_SFC, true, false, "SNES"},
            {"Donkey Kong Country", "Plataformas con gráficos revolucionarios.", null, SNES_DIR + "dkc" + EXT_SFC, true, true, "SNES"},
            {"Chrono Trigger", "JRPG de culto con viajes en el tiempo.", null, SNES_DIR + "chrono_trigger" + EXT_SFC, true, false, "SNES"},
            {"Street Fighter II", "Clásico de lucha 2D.", null, SNES_DIR + "sf2" + EXT_SFC, false, true, "SNES"},

            // Game Boy
            {"Tetris", "El clásico puzzle por excelencia.", null, GB_DIR + "tetris" + EXT_GB, false, true, GAME_BOY},
            {"Pokémon Red", "Comienza tu viaje para atraparlos a todos.", null, GB_DIR + "pokemon_red" + EXT_GB, true, false, GAME_BOY},
            {"Kirby’s Dream Land", "Debut del personaje rosado favorito.", null, GB_DIR + "kirby" + EXT_GB, false, false, GAME_BOY},
            {"Dr. Mario", "Puzzle de cápsulas medicinales.", null, GB_DIR + "dr_mario" + EXT_GB, false, true, GAME_BOY},

            // Game Boy Advance
            {"Advance Wars", "Estrategia militar por turnos.", null, GBA_DIR + "advance_wars" + EXT_GBA, false, false, GAME_BOY_ADVANCE},
            {"Metroid Fusion", "Exploración y acción en un mundo alienígena.", null, GBA_DIR + "metroid_fusion" + EXT_GBA, true, true, GAME_BOY_ADVANCE},
            {"The Legend of Zelda: The Minish Cap", "Una aventura diminuta.", null, GBA_DIR + "minish_cap" + EXT_GBA, true, false, GAME_BOY_ADVANCE},
            {"Mario Kart: Super Circuit", "Velocidad y caos sobre ruedas.", null, GBA_DIR + "mksc" + EXT_GBA, true, true, GAME_BOY_ADVANCE},

            // Genesis
            {"Sonic the Hedgehog", "El erizo azul veloz.", null, GENESIS_DIR + "sonic" + EXT_GEN, false, true, GENESIS},
            {"Streets of Rage", "Beat 'em up callejero.", null, GENESIS_DIR + "sor" + EXT_GEN, false, false, GENESIS},
            {"Golden Axe", "Espada y brujería en scroll lateral.", null, GENESIS_DIR + "golden_axe" + EXT_GEN, true, false, GENESIS},
            {"Gunstar Heroes", "Run and gun frenético.", null, GENESIS_DIR + "gunstar_heroes" + EXT_GEN, false, true, GENESIS},

            // Nintendo 64
            {"Super Mario 64", "Salto a las 3D de Mario.", null, N64_DIR + "super_mario_64" + EXT_N64, true, true, NINTENDO_64},
            {"The Legend of Zelda: Ocarina of Time", "Una de las mejores aventuras jamás creadas.", null, N64_DIR + "ocarina" + EXT_N64, true, true, NINTENDO_64},
            {"GoldenEye 007", "FPS multijugador mítico.", null, N64_DIR + "goldeneye" + EXT_N64, false, true, NINTENDO_64},
            {"Banjo-Kazooie", "Plataformas 3D con humor y coleccionables.", null, N64_DIR + "banjo" + EXT_N64, true, false, NINTENDO_64},
    };

    /**
     * Inicializa la base de datos con plataformas y ROMs por defecto si esta se encuentra vacía.
     *
     * <p>Este método se ejecuta al iniciar la aplicación y asegura que la base de datos
     * siempre cuente con un conjunto mínimo de datos.</p>
     */
    public static void initializeWithDefaults() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            long plataformasCount = em.createQuery("SELECT COUNT(p) FROM Plataforma p", Long.class)
                    .getSingleResult();
            if (plataformasCount == 0) {
                logger.info("No platforms found. Inserting default platforms...");
                insertDefaultPlatforms(em);
            }

            long romsCount = em.createQuery("SELECT COUNT(r) FROM Rom r", Long.class)
                    .getSingleResult();
            if (romsCount == 0) {
                logger.info("No ROMs found. Inserting default ROMs...");
                insertDefaultRoms(em);
            }

            em.getTransaction().commit();
            logger.info("Default data initialization completed.");

        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.error("Error during default data initialization", e);
        } finally {
            em.close();
        }
    }

    /**
     * Inserta todas las plataformas por defecto.
     *
     * @param em EntityManager activo dentro de una transacción abierta.
     */
    private static void insertDefaultPlatforms(EntityManager em) {
        for (String[] p : DEFAULT_PLATFORMS) {
            em.persist(new Plataforma(p[0], p[1], p[2]));
            logger.debug("Inserted default platform: {} ({})", p[0], p[1]);
        }
        logger.info("Default platforms inserted.");
    }

    /**
     * Inserta plataformas por defecto solo si no existen previamente.
     *
     * <p>Útil para operaciones donde se quiere garantizar la existencia
     * de plataformas base sin sobrescribir datos existentes.</p>
     */
    public static void insertOrUpdateDefaultPlatforms() {
        EntityManager em = JpaUtil.getEntityManager();

        try {
            em.getTransaction().begin();

            long count = em.createQuery("SELECT COUNT(p) FROM Plataforma p", Long.class)
                    .getSingleResult();

            if (count > 0) {
                logger.info("Default platforms already exist, skipping creation.");
                em.getTransaction().commit();
                return;
            }

            logger.info("No platforms detected. Creating default platform set...");

            for (String[] p : DEFAULT_PLATFORMS) {
                em.persist(new Plataforma(p[0], p[1], p[2]));
                logger.info("Inserted default platform: {}", p[0]);
            }

            em.getTransaction().commit();

        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.error("Error inserting default platforms", e);
        } finally {
            em.close();
        }
    }

    /**
     * Inserta los ROMs de ejemplo asociados a las plataformas por defecto.
     *
     * @param em EntityManager activo dentro de una transacción abierta.
     */
    private static void insertDefaultRoms(EntityManager em) {
        Map<String, Plataforma> plataformas = new HashMap<>();
        em.createQuery("SELECT p FROM Plataforma p", Plataforma.class)
                .getResultList()
                .forEach(p -> plataformas.put(p.getNombre(), p));

        for (Object[] r : DEFAULT_ROMS) {
            Plataforma plataforma = plataformas.get((String) r[6]);

            if (plataforma == null) {
                logger.warn("Platform not found for ROM: {}", r[0]);
                continue;
            }

            Rom rom = new Rom(
                    (String) r[0], // título
                    (String) r[1], // descripción
                    (String) r[3], // ruta
                    (String) r[2], // imagen
                    (Boolean) r[4], // favorito
                    (Boolean) r[5], // jugado
                    plataforma
            );

            em.persist(rom);
            logger.debug("Inserted default ROM: {} for platform {}", r[0], plataforma.getNombre());
        }

        logger.info("Default ROMs inserted.");
    }
}
