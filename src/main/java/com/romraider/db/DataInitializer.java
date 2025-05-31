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
                {"NES", ".nes", "roms/nes"},
                {"SNES", ".sfc", "roms/snes"},
                {"Game Boy", ".gb", "roms/gb"},
                {"Game Boy Advance", ".gba", "roms/gba"},
                {"Genesis", ".gen", "roms/genesis"},
                {"Nintendo 64", ".n64", "roms/n64"}
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
                {"Super Mario Bros.", "Clásico de plataformas de Nintendo.", null, true, true, "NES"},
                {"The Legend of Zelda", "Aventura de exploración y acción.", null, true, false, "NES"},
                {"Metroid", "Exploración alienígena con acción y plataformas.", null, false, true, "NES"},
                {"Castlevania", "Acción y horror gótico en 8 bits.", null, false, false, "NES"},

                // SNES
                {"Super Mario World", "Colorido y amplio juego de plataformas.", null, true, false, "SNES"},
                {"Donkey Kong Country", "Plataformas con gráficos revolucionarios.", null, true, true, "SNES"},
                {"Chrono Trigger", "JRPG de culto con viajes en el tiempo.", null, true, false, "SNES"},
                {"Street Fighter II", "Clásico de lucha 2D.", null, false, true, "SNES"},

                // Game Boy
                {"Tetris", "El clásico puzzle por excelencia.", null, false, true, "Game Boy"},
                {"Pokémon Red", "Comienza tu viaje para atraparlos a todos.", null, true, false, "Game Boy"},
                {"Kirby’s Dream Land", "Debut del personaje rosado favorito.", null, false, false, "Game Boy"},
                {"Dr. Mario", "Puzzle de cápsulas medicinales.", null, false, true, "Game Boy"},

                // Game Boy Advance
                {"Advance Wars", "Estrategia militar por turnos.", null, false, false, "Game Boy Advance"},
                {"Metroid Fusion", "Exploración y acción en un mundo alienígena.", null, true, true, "Game Boy Advance"},
                {"The Legend of Zelda: The Minish Cap", "Una aventura diminuta.", null, true, false, "Game Boy Advance"},
                {"Mario Kart: Super Circuit", "Velocidad y caos sobre ruedas.", null, true, true, "Game Boy Advance"},

                // Genesis
                {"Sonic the Hedgehog", "El erizo azul veloz.", null, false, true, "Genesis"},
                {"Streets of Rage", "Beat 'em up callejero.", null, false, false, "Genesis"},
                {"Golden Axe", "Espada y brujería en scroll lateral.", null, true, false, "Genesis"},
                {"Gunstar Heroes", "Run and gun frenético.", null, false, true, "Genesis"},

                // Nintendo 64
                {"Super Mario 64", "Salto a las 3D de Mario.", null, true, true, "Nintendo 64"},
                {"The Legend of Zelda: Ocarina of Time", "Una de las mejores aventuras jamás creadas.", null, true, true, "Nintendo 64"},
                {"GoldenEye 007", "FPS multijugador mítico.", null, false, true, "Nintendo 64"},
                {"Banjo-Kazooie", "Plataformas 3D con humor y coleccionables.", null, true, false, "Nintendo 64"},
        };

        for (Object[] r : roms) {
            Plataforma plataforma = plataformas.get((String) r[5]);
            if (plataforma != null) {
                Rom rom = new Rom(
                        (String) r[0],
                        (String) r[1],
                        (String) r[2],
                        (Boolean) r[3],
                        (Boolean) r[4],
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
