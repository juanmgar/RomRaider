package com.romraider.repository;

import com.romraider.model.Plataforma;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class PlataformaRepository {

    private final EntityManager em;

    public PlataformaRepository(EntityManager em) {
        this.em = em;
    }

    public List<Plataforma> findAll() {
        TypedQuery<Plataforma> query = em.createQuery(
                "SELECT p FROM Plataforma p ORDER BY p.nombre", Plataforma.class);
        return query.getResultList();
    }

    public Plataforma findById(int id) {
        return em.find(Plataforma.class, id);
    }

    public void save(Plataforma plataforma) {
        if (plataforma.getId() == 0) {
            em.persist(plataforma);
        } else {
            em.merge(plataforma);
        }
    }

    public void delete(int id) {
        Plataforma plataforma = em.find(Plataforma.class, id);
        if (plataforma != null) {
            em.remove(plataforma);
        }
    }
}
