package com.romraider.db;

import com.romraider.utils.AppInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class JpaUtil {
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    private static EntityManagerFactory buildEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();

        // Ruta absoluta al archivo de la base de datos dentro de ~/.romraider/db
        String dbPath = AppInitializer.dbDir.resolve("romraider").toAbsolutePath().toString();

        // Configuración dinámica
        properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:" + dbPath);
        properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.show_sql", "true");

        return Persistence.createEntityManagerFactory("romraiderPU", properties);
    }
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        if (emf.isOpen()) {
            emf.close();
        }
    }
}
