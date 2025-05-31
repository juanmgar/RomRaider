package com.romraider.db;

import com.romraider.utils.AppInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JpaUtil {

    private static final Logger logger = LoggerFactory.getLogger(JpaUtil.class);
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    private static EntityManagerFactory buildEntityManagerFactory() {
        Map<String, Object> properties = new HashMap<>();

        String dbPath = AppInitializer.dbDir.resolve("romraider").toAbsolutePath().toString();
        logger.info("Configuring H2 database at: {}", dbPath);

        try {
            // Configuración dinámica
            properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:" + dbPath);
            properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", "true");

            logger.debug("JPA properties set: {}", properties);
            return Persistence.createEntityManagerFactory("romraiderPU", properties);

        } catch (Exception e) {
            logger.error("Error initializing EntityManagerFactory", e);
            throw new RuntimeException("Failed to initialize EntityManagerFactory", e);
        }
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        if (emf.isOpen()) {
            emf.close();
            logger.info("EntityManagerFactory closed successfully");
        }
    }
}
