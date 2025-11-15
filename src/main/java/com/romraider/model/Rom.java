package com.romraider.model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.*;

/**
 * Entidad que representa un ROM dentro de una plataforma.
 *
 * <p>Incluye información sobre:</p>
 * <ul>
 *     <li>Título del juego</li>
 *     <li>Descripción</li>
 *     <li>Ruta de imagen local</li>
 *     <li>Marcadores (favorito / jugado)</li>
 *     <li>Plataforma asociada</li>
 * </ul>
 *
 * <p>Se excluye del XML la referencia a plataforma para evitar
 * ciclos recursivos durante la exportación.</p>
 */
@Entity
@Table(name = "roms")
@XmlRootElement(name = "rom")
@XmlAccessorType(XmlAccessType.FIELD)
public class Rom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @XmlTransient
    private int id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 4000)
    private String descripcion;

    private String imagen;

    private boolean favorito;

    private boolean jugado;

    @ManyToOne
    @JoinColumn(name = "plataforma_id", nullable = false)
    @XmlTransient
    private Plataforma plataforma;

    public Rom() {
    }

    public Rom(String titulo, String descripcion, String imagen, boolean favorito, boolean jugado, Plataforma plataforma) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.favorito = favorito;
        this.jugado = jugado;
        this.plataforma = plataforma;
    }

    public Rom(int id, String titulo, String descripcion, String imagen, boolean favorito, boolean jugado, Plataforma plataforma) {
        this(titulo, descripcion, imagen, favorito, jugado, plataforma);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public boolean isFavorito() {
        return favorito;
    }

    public boolean isJugado() {
        return jugado;
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }

    public void setJugado(boolean jugado) {
        this.jugado = jugado;
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
