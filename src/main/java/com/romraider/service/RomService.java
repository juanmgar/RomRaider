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
 * <p>Esta capa se sitúa entre los controladores y el repositorio,
 * garantizando un acceso consistente a la base de datos mediante:</p>
 *
 * <ul>
 *     <li>Gestión controlada del {@link EntityManager}</li>
 *     <li>Manejo explícito de transacciones en operaciones de escritura</li>
 *     <li>Notificación de cambios locales vía {@link SyncStateUtils}</li>
 * </ul>
 *
 * <p>No añade lógica de negocio compleja: su misión principal es orquestar
 * las operaciones de persistencia.</p>
 */
public class RomService {

    /**
     * Obtiene todos los ROMs almacenados en la base de datos.
     *
     * @return lista completa de ROMs.
     */
    public List<Rom> obtenerTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        List<Rom> result = new RomRepository(em).findAll();
        em.close();
        return result;
    }

    /**
     * Comprueba si existe un ROM con un título específico dentro de una plataforma concreta.
     * La comparación es case-insensitive.
     *
     * @param titulo       título del ROM a buscar.
     * @param plataformaId identificador de la plataforma.
     * @return {@code true} si ya existe un ROM con ese título en esa plataforma,
     *         {@code false} en caso contrario.
     */
    public boolean existeRomConTituloYPlataforma(String titulo, int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        boolean result = new RomRepository(em).existsByTituloAndPlataformaId(titulo, plataformaId);
        em.close();
        return result;
    }

    /**
     * Busca un ROM por su identificador único.
     *
     * @param id identificador del ROM.
     * @return instancia encontrada o {@code null} si no existe.
     */
    public Rom buscarPorId(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        Rom result = new RomRepository(em).findById(id);
        em.close();
        return result;
    }

    /**
     * Obtiene todos los ROMs asociados a una plataforma concreta.
     *
     * @param plataformaId ID de la plataforma.
     * @return lista de ROMs pertenecientes a esa plataforma.
     */
    public List<Rom> obtenerPorPlataforma(int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        List<Rom> result = new RomRepository(em).findByPlataformaId(plataformaId);
        em.close();
        return result;
    }

    /**
     * Guarda o actualiza un ROM en la base de datos.
     *
     * <p>Tras la persistencia, se notifica al sistema de sincronización
     * que existen cambios locales pendientes de subir.</p>
     *
     * @param rom entidad que se desea persistir.
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
     * @param id identificador del ROM a borrar.
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
     * @param plataformaId ID de la plataforma cuyos ROMs serán eliminados.
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
