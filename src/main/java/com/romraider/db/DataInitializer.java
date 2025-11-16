package com.romraider.db;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final String GAME_BOY = "Game Boy";
    private static final String GAME_BOY_ADVANCE = "Game Boy Advance";
    private static final String GENESIS = "Genesis";

    public static void initializeWithDefaults() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            long plataformasCount = em.createQuery("SELECT COUNT(p) FROM Plataforma p", Long.class).getSingleResult();
            if (plataformasCount == 0) {
                logger.info("No platforms found. Inserting default platforms...");
                insertDefaultPlatforms(em);
            } else {
                logger.debug("Existing platforms found: {}", plataformasCount);
            }

            long romsCount = em.createQuery("SELECT COUNT(r) FROM Rom r", Long.class).getSingleResult();
            if (romsCount == 0) {
                logger.info("No ROMs found. Inserting default ROMs...");
                insertDefaultRoms(em);
            } else {
                logger.debug("Existing ROMs found: {}", romsCount);
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

    private static void insertDefaultPlatforms(EntityManager em) {
        String[][] platforms = {
                // Nintendo
                {"NES", ".nes", "nes"},
                {"SNES", ".sfc,.smc", "snes"},
                {"Nintendo 64", ".n64,.z64,.v64", "n64"},
                {"GameCube", ".iso,.gcm", "gamecube"},
                {"Wii", ".iso,.wbfs", "wii"},
                {"Game Boy", ".gb", "gb"},
                {"Game Boy Color", ".gbc", "gbc"},
                {"Game Boy Advance", ".gba", "gba"},
                {"Nintendo DS", ".nds", "nds"},
                {"Nintendo 3DS", ".3ds,.cia", "3ds"},
                {"Nintendo Switch", ".nsp,.xci", "switch"},

                // Sega
                {"Master System", ".sms", "sms"},
                {"Genesis / Mega Drive", ".md,.gen", "genesis"},
                {"Sega CD", ".cue,.bin", "segacd"},
                {"Sega 32X", ".32x", "32x"},
                {"Sega Saturn", ".cue,.bin", "saturn"},
                {"Dreamcast", ".cdi,.gdi", "dreamcast"},

                // Sony
                {"PlayStation", ".cue,.bin,.iso", "psx"},
                {"PlayStation 2", ".iso", "ps2"},
                {"PSP", ".iso,.cso", "psp"},

                // Atari & Retro
                {"Atari 2600", ".a26", "atari2600"},
                {"Atari 7800", ".a78", "atari7800"},
                {"Neo Geo", ".zip", "neogeo"},
                {"MAME", ".zip", "mame"},

                // PC
                {"MS-DOS", ".exe,.com,.zip", "msdos"}
        };

        for (String[] p : platforms) {
            Plataforma plat = new Plataforma(p[0], p[1], p[2]);
            em.persist(plat);
            logger.debug("Inserted default platform: {} ({})", p[0], p[1]);
        }

        logger.info("Default platforms inserted.");
    }

    private static void insertDefaultRoms(EntityManager em) {
        Map<String, Plataforma> plataformas = new HashMap<>();
        em.createQuery("SELECT p FROM Plataforma p", Plataforma.class)
                .getResultList()
                .forEach(p -> plataformas.put(p.getNombre(), p));

        Object[][] roms = {
                // NES
                {"Super Mario Bros.", "Clásico de plataformas de Nintendo.", null, "/roms/nes/super_mario_bros.nes", true, true, "NES"},
                {"The Legend of Zelda", "Aventura de exploración y acción.", null, "/roms/nes/zelda.nes", true, false, "NES"},
                {"Metroid", "Exploración alienígena con acción y plataformas.", null, "/roms/nes/metroid.nes", false, true, "NES"},
                {"Castlevania", "Acción y horror gótico en 8 bits.", null, "/roms/nes/castlevania.nes", false, false, "NES"},

                // SNES
                {"Super Mario World", "Colorido y amplio juego de plataformas.", null, "/roms/snes/super_mario_world.sfc", true, false, "SNES"},
                {"Donkey Kong Country", "Plataformas con gráficos revolucionarios.", null, "/roms/snes/dkc.sfc", true, true, "SNES"},
                {"Chrono Trigger", "JRPG de culto con viajes en el tiempo.", null, "/roms/snes/chrono_trigger.sfc", true, false, "SNES"},
                {"Street Fighter II", "Clásico de lucha 2D.", null, "/roms/snes/sf2.sfc", false, true, "SNES"},

                // Game Boy
                {"Tetris", "El clásico puzzle por excelencia.", null, "/roms/gb/tetris.gb", false, true, "Game Boy"},
                {"Pokémon Red", "Comienza tu viaje para atraparlos a todos.", null, "/roms/gb/pokemon_red.gb", true, false, "Game Boy"},
                {"Kirby’s Dream Land", "Debut del personaje rosado favorito.", null, "/roms/gb/kirby.gb", false, false, "Game Boy"},
                {"Dr. Mario", "Puzzle de cápsulas medicinales.", null, "/roms/gb/dr_mario.gb", false, true, "Game Boy"},

                // Game Boy Advance
                {"Advance Wars", "Estrategia militar por turnos.", null, "/roms/gba/advance_wars.gba", false, false, "Game Boy Advance"},
                {"Metroid Fusion", "Exploración y acción en un mundo alienígena.", null, "/roms/gba/metroid_fusion.gba", true, true, "Game Boy Advance"},
                {"The Legend of Zelda: The Minish Cap", "Una aventura diminuta.", null, "/roms/gba/minish_cap.gba", true, false, "Game Boy Advance"},
                {"Mario Kart: Super Circuit", "Velocidad y caos sobre ruedas.", null, "/roms/gba/mksc.gba", true, true, "Game Boy Advance"},

                // Genesis
                {"Sonic the Hedgehog", "El erizo azul veloz.", null, "/roms/genesis/sonic.gen", false, true, "Genesis"},
                {"Streets of Rage", "Beat 'em up callejero.", null, "/roms/genesis/sor.gen", false, false, "Genesis"},
                {"Golden Axe", "Espada y brujería en scroll lateral.", null, "/roms/genesis/golden_axe.gen", true, false, "Genesis"},
                {"Gunstar Heroes", "Run and gun frenético.", null, "/roms/genesis/gunstar_heroes.gen", false, true, "Genesis"},

                // Nintendo 64
                {"Super Mario 64", "Salto a las 3D de Mario.", null, "/roms/n64/super_mario_64.n64", true, true, "Nintendo 64"},
                {"The Legend of Zelda: Ocarina of Time", "Una de las mejores aventuras jamás creadas.", null, "/roms/n64/ocarina.n64", true, true, "Nintendo 64"},
                {"GoldenEye 007", "FPS multijugador mítico.", null, "/roms/n64/goldeneye.n64", false, true, "Nintendo 64"},
                {"Banjo-Kazooie", "Plataformas 3D con humor y coleccionables.", null, "/roms/n64/banjo.n64", true, false, "Nintendo 64"},
        };

        for (Object[] r : roms) {
            Plataforma plataforma = plataformas.get((String) r[6]);
            if (plataforma != null) {
                Rom rom = new Rom(
                        (String) r[0],   // título
                        (String) r[1],   // descripción
                        (String) r[3],   // imagen (puede ser null)
                        (String) r[2],   // ruta REAL añadida
                        (Boolean) r[4],  // favorito
                        (Boolean) r[5],  // jugado
                        plataforma
                );
                em.persist(rom);
                logger.debug("Inserted default ROM: {} for platform {}", r[0], plataforma.getNombre());
            } else {
                logger.warn("Platform not found for ROM: {}", r[0]);
            }
        }

        logger.info("ROMs por defecto insertadas.");
    }

}
