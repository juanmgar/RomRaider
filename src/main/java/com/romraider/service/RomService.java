package com.romraider.service;

import com.romraider.db.JpaUtil;
import com.romraider.model.Rom;
import com.romraider.repository.RomRepository;
import jakarta.persistence.EntityManager;

import java.util.List;

public class RomService {

    public List<Rom> obtenerTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        List<Rom> result = new RomRepository(em).findAll();
        em.close();
        return result;
    }

    public boolean existeRomConTituloYPlataforma(String titulo, int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        boolean result = new RomRepository(em).existsByTituloAndPlataformaId(titulo, plataformaId);
        em.close();
        return result;
    }

    public Rom buscarPorId(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        Rom result = new RomRepository(em).findById(id);
        em.close();
        return result;
    }

    public List<Rom> obtenerPorPlataforma(int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        List<Rom> result = new RomRepository(em).findByPlataformaId(plataformaId);
        em.close();
        return result;
    }

    public void guardar(Rom rom) {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);
        em.getTransaction().begin();
        repo.save(rom);
        em.getTransaction().commit();
        em.close();
    }

    public void eliminar(int id) {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);
        em.getTransaction().begin();
        repo.delete(id);
        em.getTransaction().commit();
        em.close();
    }

    public void eliminarPorPlataforma(int plataformaId) {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);
        em.getTransaction().begin();
        repo.deleteByPlataformaId(plataformaId);
        em.getTransaction().commit();
        em.close();
    }

    public void eliminarTodas() {
        EntityManager em = JpaUtil.getEntityManager();
        RomRepository repo = new RomRepository(em);
        em.getTransaction().begin();
        repo.deleteAll();
        em.getTransaction().commit();
        em.close();
    }
}
