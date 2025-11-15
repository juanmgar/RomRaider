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
 * gestionando EntityManager, transacciones y notificando cambios locales
 * a través de {@link SyncStateUtils}.</p>
 *
 * <p>No contiene lógica de negocio compleja, pero garantiza el patrón
 * de acceso consistente a datos.</p>
 */
public class PlataformaService {

    /**
     * Obtiene una lista de todas las plataformas registradas,
     * ordenadas alfabéticamente por nombre.
     *
     * @return lista de plataformas
     */
    public List<Plataforma> obtenerTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        List<Plataforma> result = new PlataformaRepository(em).findAll();
        em.close();
        return result;
    }

    /**
     * Obtiene todas las plataformas incluyendo sus ROMs asociados.
     *
     * <p>Utiliza una consulta con FETCH JOIN para evitar problemas
     * de inicialización perezosa al exportar o mostrar estadísticas.</p>
     *
     * @return lista de plataformas con ROMs cargados
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
     * @param id identificador de la plataforma
     * @return plataforma encontrada o null si no existe
     */
    public Plataforma buscarPorId(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        Plataforma result = new PlataformaRepository(em).findById(id);
        em.close();
        return result;
    }

    /**
     * Guarda (crea o actualiza) una plataforma.
     *
     * <p>Gestiona la transacción y marca el estado local como modificado
     * para que el sistema de sincronización lo detecte.</p>
     *
     * @param plataforma entidad a persistir
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
     * @param id identificador de la plataforma a eliminar
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
     * <p>Utilizado principalmente en procesos de importación y modo offline.</p>
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
