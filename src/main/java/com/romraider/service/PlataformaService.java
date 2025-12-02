package com.romraider.service;

import com.romraider.db.JpaUtil;
import com.romraider.model.Plataforma;
import com.romraider.repository.PlataformaRepository;
import com.romraider.utils.SyncStateUtils;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Servicio que encapsula la lógica relacionada con la gestión de plataformas.
 *
 * <p>Esta capa actúa como intermediario entre controladores y repositorios,
 * gestionando {@link EntityManager}, transacciones y notificando cambios locales
 * a través de {@link SyncStateUtils}.</p>
 *
 * <p>No contiene lógica de negocio compleja, pero garantiza un acceso
 * consistente y centralizado a los datos relacionados con plataformas.</p>
 */
public class PlataformaService {

    /**
     * Obtiene una lista de todas las plataformas registradas,
     * ordenadas alfabéticamente por nombre.
     *
     * @return lista completa de plataformas existentes.
     */
    public List<Plataforma> obtenerTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        List<Plataforma> result = new PlataformaRepository(em).findAll();
        em.close();
        return result;
    }

    /**
     * Obtiene todas las plataformas incluyendo sus ROMs asociadas.
     *
     * <p>Utiliza una consulta con FETCH JOIN para evitar problemas
     * de inicialización perezosa (LazyInitializationException) al exportar
     * o al mostrar información agregada.</p>
     *
     * @return lista de plataformas con sus ROMs cargados.
     */
    public List<Plataforma> obtenerTodasConRoms() {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);
        List<Plataforma> result = repo.findAllWithRoms();
        em.close();
        return result;
    }

    /**
     * Busca una plataforma por su ID.
     *
     * @param id identificador único de la plataforma.
     * @return la plataforma encontrada o {@code null} si no existe.
     */
    public Plataforma buscarPorId(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        Plataforma result = new PlataformaRepository(em).findById(id);
        em.close();
        return result;
    }

    /**
     * Guarda (crea o actualiza) una plataforma en la base de datos.
     *
     * <p>Gestiona la transacción de forma explícita y marca el estado
     * local como modificado para que el sistema de sincronización
     * detecte el cambio.</p>
     *
     * @param plataforma entidad a persistir.
     */
    public void guardar(Plataforma plataforma) {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);

        em.getTransaction().begin();
        repo.save(plataforma);
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }

    /**
     * Elimina una plataforma por su ID.
     *
     * <p>La operación marca el estado local como modificado.</p>
     *
     * @param id identificador de la plataforma a eliminar.
     */
    public void eliminar(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);

        em.getTransaction().begin();
        repo.delete(id);
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }

    /**
     * Elimina todas las plataformas y ROMs asociadas.
     *
     * <p>Utilizado principalmente durante procesos de importación masiva
     * o cuando se desea restaurar el estado inicial en modo offline.</p>
     */
    public void eliminarTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);

        em.getTransaction().begin();
        repo.deleteAll();
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }
}
