package com.romraider.repository;

import com.romraider.model.Rom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

/**
 * Repositorio encargado de realizar operaciones de persistencia sobre la entidad {@link Rom}.
 *
 * <p>
 * Gestiona:
 * <ul>
 *     <li>Búsquedas generales y filtradas</li>
 *     <li>Persistencia y actualización</li>
 *     <li>Eliminación individual y masiva</li>
 *     <li>Consultas específicas por plataforma</li>
 *     <li>Verificación de duplicados por título y plataforma</li>
 * </ul>
 *
 * Esta capa NO debe contener lógica de negocio; únicamente consultas y operaciones
 * directas sobre el {@link EntityManager}.
 */
public class RomRepository {

    /** EntityManager proporcionado externamente y gestionado por la capa superior. */
    private final EntityManager em;

    /**
     * Constructor que recibe el EntityManager a utilizar.
     *
     * @param em gestor de entidades JPA activo, usado para todas las operaciones del repositorio.
     */
    public RomRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Obtiene todos los ROMs almacenados.
     *
     * @return lista completa de ROMs.
     */
    public List<Rom> findAll() {
        return em.createQuery("SELECT r FROM Rom r", Rom.class)
                .getResultList();
    }

    /**
     * Busca un ROM por su identificador único.
     *
     * @param id identificador del ROM.
     * @return la entidad encontrada o {@code null} si no existe.
     */
    public Rom findById(int id) {
        return em.find(Rom.class, id);
    }

    /**
     * Comprueba si existe un ROM con un título (case-insensitive)
     * dentro de una plataforma concreta.
     *
     * <p>
     * Se utiliza una consulta con {@code COUNT} para evitar cargar entidades completas
     * innecesariamente.
     * </p>
     *
     * @param titulo título del ROM a verificar.
     * @param plataformaId ID de la plataforma asociada.
     * @return {@code true} si existe un ROM duplicado, {@code false} si no.
     */
    public boolean existsByTituloAndPlataformaId(String titulo, int plataformaId) {
        Long count = em.createQuery(
                        "SELECT COUNT(r) FROM Rom r " +
                                "WHERE LOWER(r.titulo) = :titulo AND r.plataforma.id = :plataformaId",
                        Long.class)
                .setParameter("titulo", titulo.toLowerCase())
                .setParameter("plataformaId", plataformaId)
                .getSingleResult();

        return count > 0;
    }

    /**
     * Obtiene todos los ROMs asociados a una plataforma específica.
     *
     * @param plataformaId ID de la plataforma.
     * @return lista de ROMs pertenecientes a dicha plataforma.
     */
    public List<Rom> findByPlataformaId(int plataformaId) {
        TypedQuery<Rom> query = em.createQuery(
                "SELECT r FROM Rom r WHERE r.plataforma.id = :id",
                Rom.class
        );
        query.setParameter("id", plataformaId);
        return query.getResultList();
    }

    /**
     * Persiste un nuevo ROM o actualiza uno existente.
     *
     * Convención usada:
     * <ul>
     *     <li>ID = 0 -> entidad nueva -> {@link EntityManager#persist(Object)}</li>
     *     <li>ID &gt; 0 -> entidad existente -> {@link EntityManager#merge(Object)}</li>
     * </ul>
     *
     * @param rom entidad ROM a guardar.
     */
    public void save(Rom rom) {
        if (rom.getId() == 0) {
            em.persist(rom);
        } else {
            em.merge(rom);
        }
    }

    /**
     * Elimina un ROM por su ID si existe.
     *
     * @param id identificador del ROM.
     */
    public void delete(int id) {
        Rom rom = em.find(Rom.class, id);
        if (rom != null) {
            em.remove(rom);
        }
    }

    /**
     * Elimina todos los ROMs de la base de datos.
     */
    public void deleteAll() {
        em.createQuery("DELETE FROM Rom").executeUpdate();
    }

    /**
     * Elimina todos los ROMs pertenecientes a una plataforma concreta.
     *
     * @param plataformaId ID de la plataforma cuyos ROMs deben eliminarse.
     */
    public void deleteByPlataformaId(int plataformaId) {
        em.createQuery(
                        "DELETE FROM Rom r WHERE r.plataforma.id = :id")
                .setParameter("id", plataformaId)
                .executeUpdate();
    }

}
