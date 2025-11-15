package com.romraider.service;

import com.romraider.db.JpaUtil;
import com.romraider.model.Rom;
import com.romraider.repository.RomRepository;
import com.romraider.utils.SyncStateUtils;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Servicio encargado de gestionar operaciones relacionadas con los ROMs.
 *
 * <p>
 * Esta capa actúa como intermediario entre controladores y repositorios,
 * proporcionando:
 * </p>
 *
 * <ul>
 *     <li>Gestión controlada del EntityManager</li>
 *     <li>Manejo de transacciones en operaciones de escritura</li>
 *     <li>Notificación de cambios locales al sistema de sincronización</li>
 * </ul>
 *
 * <p>No incluye lógica de negocio más allá de delegar en el repositorio.</p>
 */
public class RomService {

    /**
     * Obtiene todos los ROMs almacenados.
     *
     * @return lista de ROMs
     */
    public List<Rom> obtenerTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        List<Rom> result = new RomRepository(em).findAll();
        em.close();
        return result;
    }

    /**
     * Comprueba si existe un ROM que coincida con un título concreto
     * dentro de una plataforma específica (case-insensitive).
     *
     * @param titulo título del ROM
     * @param plataformaId ID de la plataforma
     * @return true si existe un duplicado, false si no
     */
    public boolean existeRomConTituloYPlataforma(String titulo, int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        boolean result = new RomRepository(em).existsByTituloAndPlataformaId(titulo, plataformaId);
        em.close();
        return result;
    }

    /**
     * Busca un ROM por su ID.
     *
     * @param id identificador del ROM
     * @return instancia encontrada o null si no existe
     */
    public Rom buscarPorId(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        Rom result = new RomRepository(em).findById(id);
        em.close();
        return result;
    }

    /**
     * Obtiene todos los ROMs pertenecientes a una plataforma concreta.
     *
     * @param plataformaId ID de la plataforma
     * @return lista de ROMs asociados
     */
    public List<Rom> obtenerPorPlataforma(int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        List<Rom> result = new RomRepository(em).findByPlataformaId(plataformaId);
        em.close();
        return result;
    }

    /**
     * Guarda o actualiza un ROM.
     *
     * <p>Gestiona la transacción e informa al sistema de sincronización
     * de que existen cambios locales pendientes.</p>
     *
     * @param rom entidad a persistir
     */
    public void guardar(Rom rom) {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);

        em.getTransaction().begin();
        repo.save(rom);
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }

    /**
     * Elimina un ROM por su ID.
     *
     * @param id identificador del ROM a borrar
     */
    public void eliminar(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);

        em.getTransaction().begin();
        repo.delete(id);
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }

    /**
     * Elimina todos los ROMs asociados a una plataforma concreta.
     *
     * @param plataformaId ID de la plataforma cuyos ROMs serán eliminados
     */
    public void eliminarPorPlataforma(int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);

        em.getTransaction().begin();
        repo.deleteByPlataformaId(plataformaId);
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }

    /**
     * Elimina todos los ROMs de la base de datos.
     */
    public void eliminarTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);

        em.getTransaction().begin();
        repo.deleteAll();
        em.getTransaction().commit();
        em.close();

        SyncStateUtils.markLocalChange();
    }
}
