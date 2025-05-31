package com.romraider.repository;

import com.romraider.model.Rom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class RomRepository {

    private final EntityManager em;

    public RomRepository(EntityManager em) {
        this.em = em;
    }

    public List<Rom> findAll() {
        return em.createQuery("SELECT r FROM Rom r", Rom.class).getResultList();
    }

    public Rom findById(int id) {
        return em.find(Rom.class, id);
    }

    public List<Rom> findByPlataformaId(int plataformaId) {
        TypedQuery<Rom> query = em.createQuery(
                "SELECT r FROM Rom r WHERE r.plataforma.id = :id", Rom.class);
        query.setParameter("id", plataformaId);
        return query.getResultList();
    }

    public void save(Rom rom) {
        if (rom.getId() == 0) {
            em.persist(rom);
        } else {
            em.merge(rom);
        }
    }

    public void delete(int id) {
        Rom rom = em.find(Rom.class, id);
        if (rom != null) {
            em.remove(rom);
        }
    }

    public void deleteByPlataformaId(int plataformaId) {
        em.createQuery("DELETE FROM Rom r WHERE r.plataforma.id = :id")
                .setParameter("id", plataformaId)
                .executeUpdate();
    }

}
