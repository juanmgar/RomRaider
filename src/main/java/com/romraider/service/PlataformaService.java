package com.romraider.service;

import com.romraider.db.JpaUtil;
import com.romraider.model.Plataforma;
import com.romraider.repository.PlataformaRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class PlataformaService {

    public List<Plataforma> obtenerTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        List<Plataforma> result = new PlataformaRepository(em).findAll();
        em.close();
        return result;
    }

    public List<Plataforma> obtenerTodasConRoms() {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);
        List<Plataforma> result = repo.findAllWithRoms();
        em.close();
        return result;
    }

    public Plataforma buscarPorId(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        Plataforma result = new PlataformaRepository(em).findById(id);
        em.close();
        return result;
    }

    public void guardar(Plataforma plataforma) {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);
        em.getTransaction().begin();
        repo.save(plataforma);
        em.getTransaction().commit();
        em.close();
    }

    public void eliminar(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);
        em.getTransaction().begin();
        repo.delete(id);
        em.getTransaction().commit();
        em.close();
    }

    public void eliminarTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        PlataformaRepository repo = new PlataformaRepository(em);
        em.getTransaction().begin();
        repo.deleteAll();
        em.getTransaction().commit();
        em.close();
    }

}
