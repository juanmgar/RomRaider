package com.romraider.repository;

import com.romraider.model.Plataforma;
import jakarta.persistence.EntityManager;

import java.util.List;

/**
 * Repositorio encargado de la interacción directa con la base de datos
 * para la entidad {@link Plataforma}.
 *
 * <p>
 * Este componente cumple la función de capa DAO: ejecutar consultas,
 * obtener entidades y persistir cambios. No debe contener lógica de negocio,
 * la cual pertenece a la capa de servicio.
 * </p>
 */
public class PlataformaRepository {

    /** EntityManager proporcionado externamente y gestionado por la capa superior. */
    private final EntityManager em;

    /**
     * Crea un repositorio usando el {@link EntityManager} indicado.
     *
     * @param em gestor de entidades JPA activo, responsable de las operaciones de persistencia.
     */
    public PlataformaRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Obtiene todas las plataformas ordenadas alfabéticamente por nombre.
     *
     * @return lista de plataformas existentes.
     */
    public List<Plataforma> findAll() {
        return em.createQuery(
                "SELECT p FROM Plataforma p ORDER BY p.nombre",
                Plataforma.class
        ).getResultList();
    }

    /**
     * Obtiene todas las plataformas cargando también sus ROMs asociadas.
     *
     * <p>
     * Se utiliza {@code LEFT JOIN FETCH} para evitar el problema clásico de
     * {@code LazyInitializationException} al exportar a XML o mostrar estadísticas
     * una vez cerrado el {@link EntityManager}.
     * </p>
     *
     * @return lista de plataformas con colecciones de ROMs inicializadas.
     */
    public List<Plataforma> findAllWithRoms() {
        return em.createQuery(
                "SELECT DISTINCT p FROM Plataforma p LEFT JOIN FETCH p.roms ORDER BY p.nombre",
                Plataforma.class
        ).getResultList();
    }

    /**
     * Busca una plataforma por su ID.
     *
     * @param id identificador de la plataforma.
     * @return plataforma encontrada o {@code null} si no existe.
     */
    public Plataforma findById(int id) {
        return em.find(Plataforma.class, id);
    }

    /**
     * Guarda o actualiza una plataforma dependiendo de si su ID está asignado.
     *
     * <p>
     * Convención usada:
     * <ul>
     *     <li>ID = 0 → entidad nueva → {@link EntityManager#persist(Object)}</li>
     *     <li>ID &gt; 0 → entidad existente → {@link EntityManager#merge(Object)}</li>
     * </ul>
     * </p>
     *
     * @param plataforma plataforma a persistir.
     */
    public void save(Plataforma plataforma) {
        // ID = 0 → entidad nueva
        if (plataforma.getId() == 0) {
            em.persist(plataforma);
        } else {
            em.merge(plataforma);
        }
    }

    /**
     * Elimina una plataforma por ID, si existe.
     *
     * @param id identificador de la plataforma a eliminar.
     */
    public void delete(int id) {
        Plataforma plataforma = em.find(Plataforma.class, id);
        if (plataforma != null) {
            em.remove(plataforma);
        }
    }

    /**
     * Elimina todas las plataformas y ROMs de la base de datos.
     *
     * <p>
     * El orden importa: primero se deben borrar los ROMs para evitar
     * violaciones de integridad referencial debido a la relación {@code ManyToOne}.
     * </p>
     */
    public void deleteAll() {
        em.createQuery("DELETE FROM Rom").executeUpdate();
        em.createQuery("DELETE FROM Plataforma").executeUpdate();
    }
}
