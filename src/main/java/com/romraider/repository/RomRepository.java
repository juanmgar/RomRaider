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

    public boolean existsByTituloAndPlataformaId(String titulo, int plataformaId) {
        Long count = em.createQuery(
                        "SELECT COUNT(r) FROM Rom r WHERE LOWER(r.titulo) = :titulo AND r.plataforma.id = :plataformaId",
                        Long.class)
                .setParameter("titulo", titulo.toLowerCase())
                .setParameter("plataformaId", plataformaId)
                .getSingleResult();

        return count > 0;
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

    public void deleteAll() {
        em.createQuery("DELETE FROM Rom").executeUpdate();
    }


    public void deleteByPlataformaId(int plataformaId) {
        em.createQuery("DELETE FROM Rom r WHERE r.plataforma.id = :id")
                .setParameter("id", plataformaId)
                .executeUpdate();
    }

}
