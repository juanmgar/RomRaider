package com.romraider.db;

import com.romraider.model.Plataforma;
import com.romraider.model.Rom;
import jakarta.persistence.EntityManager;

import java.util.HashMap;
import java.util.Map;

public class DataInitializer {

    public static void initializeWithDefaults() {
        EntityManager em = JpaUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            long plataformasCount = em.createQuery("SELECT COUNT(p) FROM Plataforma p", Long.class).getSingleResult();
            if (plataformasCount == 0) {
                insertDefaultPlatforms(em);
            }

            long romsCount = em.createQuery("SELECT COUNT(r) FROM Rom r", Long.class).getSingleResult();
            if (romsCount == 0) {
                insertDefaultRoms(em);
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
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
        }

        System.out.println("Plataformas por defecto insertadas.");
    }

    private static void insertDefaultRoms(EntityManager em) {
        Map<String, Plataforma> plataformas = new HashMap<>();
        em.createQuery("SELECT p FROM Plataforma p", Plataforma.class)
                .getResultList()
                .forEach(p -> plataformas.put(p.getNombre(), p));

        Object[][] roms = {
                {"Super Mario Bros.", "Clásico de plataformas de Nintendo.", null, true, true, "NES"},
                {"The Legend of Zelda", "Aventura de exploración y acción.", null, true, false, "NES"},
                {"Super Mario World", "Colorido y amplio juego de plataformas.", null, true, false, "SNES"},
                {"Tetris", "El clásico puzzle por excelencia.", null, false, true, "Game Boy"},
                {"Advance Wars", "Estrategia militar por turnos.", null, false, false, "Game Boy Advance"},
                {"Sonic the Hedgehog", "El erizo azul veloz.", null, false, true, "Genesis"},
                {"Super Mario 64", "Salto a las 3D de Mario.", null, true, true, "Nintendo 64"},
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
            }
        }

        System.out.println("ROMs por defecto insertadas.");
    }
}
