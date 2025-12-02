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
 * <p>Este componente:</p>
 * <ul>
 *     <li>Configura dinámicamente la base de datos H2 embebida.</li>
 *     <li>Inicializa un {@link EntityManagerFactory} global al inicio.</li>
 *     <li>Ofrece {@link EntityManager} bajo demanda.</li>
 *     <li>Se encarga del cierre limpio de recursos al apagar la aplicación.</li>
 * </ul>
 *
 * <p>La configuración se genera en tiempo de ejecución para permitir
 * rutas dinámicas dentro del directorio de usuario (en {@link AppInitializer#dbDir}).</p>
 */
public class JpaUtil {

    /** Logger principal para trazar la configuración y el ciclo de vida de JPA. */
    private static final Logger logger = LoggerFactory.getLogger(JpaUtil.class);

    /**
     * {@link EntityManagerFactory} global, creado una única vez durante
     * la vida de la aplicación.
     *
     * <p>Se inicializa mediante {@link #buildEntityManagerFactory()} y se
     * utiliza en {@link #getEntityManager()} para obtener gestores de entidades.</p>
     */
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    /**
     * Construye dinámicamente el {@link EntityManagerFactory} con configuración
     * específica para la base de datos H2 local.
     *
     * <p>Las propiedades se establecen en tiempo de ejecución para apuntar
     * al directorio configurado por {@link AppInitializer}, usando un
     * persistence unit llamada {@code romraiderPU}.</p>
     *
     * @return instancia configurada de {@link EntityManagerFactory}
     * @throws RuntimeException si no se puede inicializar correctamente
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
     *
     * <p>Cada llamada crea un nuevo gestor de entidades asociado al
     * {@link EntityManagerFactory} global. Es responsabilidad del
     * llamador cerrar el {@code EntityManager} después de usarlo.</p>
     *
     * @return {@link EntityManager} nuevo y listo para operar
     */
    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Cierra el {@link EntityManagerFactory} global si sigue abierto.
     *
     * <p>Debe llamarse al apagar la aplicación (por ejemplo, desde
     * {@code Application.stop()}) para liberar correctamente los recursos
     * asociados a la capa de persistencia.</p>
     */
    public static void close() {
        if (emf.isOpen()) {
            emf.close();
            logger.info("EntityManagerFactory cerrado correctamente.");
        }
    }
}
