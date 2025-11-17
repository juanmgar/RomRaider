package com.romraider.db;

import com.romraider.app.AppInitializer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilidad centralizada para gestionar JPA dentro de la aplicación.
 *
 * <p>Este componente:
 * <ul>
 *     <li>Configura dinámicamente la base de datos H2 embebida.</li>
 *     <li>Inicializa un EntityManagerFactory global al inicio.</li>
 *     <li>Ofrece EntityManager bajo demanda.</li>
 *     <li>Se encarga del cierre limpio de recursos al apagar la aplicación.</li>
 * </ul>
 *
 * <p>La configuración se genera en tiempo de ejecución para permitir
 * rutas dinámicas dentro del directorio de usuario (en AppInitializer.dbDir).</p>
 */
public class JpaUtil {

    private static final Logger logger = LoggerFactory.getLogger(JpaUtil.class);

    /** Factory global creada una única vez durante la vida de la aplicación. */
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    /**
     * Construye dinámicamente el {@link EntityManagerFactory} con configuración
     * específica para la base de datos H2 local.
     *
     * @return instancia configurada de EntityManagerFactory
     */
    private static EntityManagerFactory buildEntityManagerFactory() {

        Map<String, Object> properties = new HashMap<>();

        // Ruta absoluta hacia la BBDD local en ~/.romraider/db/romraider
        String dbPath = AppInitializer.dbDir.resolve("romraider").toAbsolutePath().toString();
        logger.info("Configurando base de datos H2 en la ruta: {}", dbPath);

        try {
            // Configuración de JDBC dinámica
            properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:" + dbPath);
            properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");

            // Dialecto específico para H2
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");

            // Auto-update para mantener el esquema al día sin perder datos
            properties.put("hibernate.hbm2ddl.auto", "update");

            // Útil en desarrollo
            properties.put("hibernate.show_sql", "true");

            logger.debug("Propiedades JPA establecidas: {}", properties);

            return Persistence.createEntityManagerFactory("romraiderPU", properties);

        } catch (Exception e) {
            logger.error("Error inicializando EntityManagerFactory", e);
            throw new RuntimeException("No se pudo inicializar EntityManagerFactory", e);
        }
    }

    /**
     * Obtiene una nueva instancia de {@link EntityManager}.
     * <p>Debe cerrarse manualmente tras su uso.</p>
     *
     * @return EntityManager nuevo y listo para operar
     */
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Cierra el EntityManagerFactory global.
     * Debe llamarse al apagar la aplicación (por ejemplo, desde Application.stop()).
     */
    public static void close() {
        if (emf.isOpen()) {
            emf.close();
            logger.info("EntityManagerFactory cerrado correctamente.");
        }
    }
}
