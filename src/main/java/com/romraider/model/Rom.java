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
 *     <li>Ruta de la ROM</li>
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

    /**
     * Identificador interno del ROM en base de datos.
     *
     * <p>No se exporta a XML para evitar exponer detalles internos.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @XmlTransient
    private int id;

    /**
     * Título del juego/ROM.
     */
    @Column(nullable = false)
    private String titulo;

    /**
     * Descripción del juego.
     * Longitud limitada para evitar problemas de almacenamiento.
     */
    @Column(length = 4000)
    private String descripcion;

    /**
     * Ruta del archivo ROM en el sistema de archivos.
     */
    @Column(nullable = false)
    private String ruta;

    /**
     * Ruta local a la imagen asociada al ROM (carátula, screenshot, etc.).
     */
    private String imagen;

    /**
     * Indicador de si el ROM está marcado como favorito.
     */
    private boolean favorito;

    /**
     * Indicador de si el ROM se ha marcado como jugado.
     */
    private boolean jugado;

    /**
     * Plataforma a la que pertenece este ROM.
     *
     * <p>Marcado como {@link XmlTransient} para evitar referencias cíclicas
     * durante la serialización XML.</p>
     */
    @ManyToOne
    @JoinColumn(name = "plataforma_id", nullable = false)
    @XmlTransient
    private Plataforma plataforma;

    /**
     * Constructor por defecto requerido por JPA y JAXB.
     */
    public Rom() {
    }

    /**
     * Crea un ROM sin ID explícito (útil para nuevas entidades).
     *
     * @param titulo      título del juego.
     * @param descripcion descripción del juego.
     * @param ruta        ruta del archivo ROM.
     * @param imagen      ruta de la imagen asociada.
     * @param favorito    si está marcado como favorito.
     * @param jugado      si está marcado como jugado.
     * @param plataforma  plataforma a la que pertenece.
     */
    public Rom(String titulo, String descripcion, String ruta, String imagen,
               boolean favorito, boolean jugado, Plataforma plataforma) {

        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ruta = ruta;
        this.imagen = imagen;
        this.favorito = favorito;
        this.jugado = jugado;
        this.plataforma = plataforma;
    }

    /**
     * Crea un ROM con un ID concreto (por ejemplo, para reconstruir desde base de datos o tests).
     *
     * @param id          identificador interno.
     * @param titulo      título del juego.
     * @param descripcion descripción del juego.
     * @param ruta        ruta del archivo ROM.
     * @param imagen      ruta de la imagen asociada.
     * @param favorito    si está marcado como favorito.
     * @param jugado      si está marcado como jugado.
     * @param plataforma  plataforma a la que pertenece.
     */
    public Rom(int id, String titulo, String descripcion, String ruta, String imagen,
               boolean favorito, boolean jugado, Plataforma plataforma) {

        this(titulo, descripcion, ruta, imagen, favorito, jugado, plataforma);
        this.id = id;
    }

    /**
     * @return identificador interno del ROM.
     */
    public int getId() {
        return id;
    }

    /**
     * @return título del ROM.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * @return descripción del ROM.
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * @return ruta del archivo ROM.
     */
    public String getRuta() {
        return ruta;
    }

    /**
     * @return ruta de la imagen asociada al ROM.
     */
    public String getImagen() {
        return imagen;
    }

    /**
     * @return {@code true} si el ROM está marcado como favorito.
     */
    public boolean isFavorito() {
        return favorito;
    }

    /**
     * @return {@code true} si el ROM está marcado como jugado.
     */
    public boolean isJugado() {
        return jugado;
    }

    /**
     * @return plataforma a la que pertenece el ROM.
     */
    public Plataforma getPlataforma() {
        return plataforma;
    }

    /**
     * Establece el identificador interno del ROM.
     *
     * @param id nuevo identificador.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Establece el título del ROM.
     *
     * @param titulo nuevo título.
     */
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    /**
     * Establece la descripción del ROM.
     *
     * @param descripcion nueva descripción.
     */
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Establece la ruta del archivo ROM.
     *
     * @param ruta nueva ruta.
     */
    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    /**
     * Establece la ruta de la imagen asociada.
     *
     * @param imagen nueva ruta de imagen.
     */
    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    /**
     * Marca o desmarca el ROM como favorito.
     *
     * @param favorito nuevo valor de favorito.
     */
    public void setFavorito(boolean favorito) {
        this.favorito = favorito;
    }

    /**
     * Marca o desmarca el ROM como jugado.
     *
     * @param jugado nuevo valor de jugado.
     */
    public void setJugado(boolean jugado) {
        this.jugado = jugado;
    }

    /**
     * Establece la plataforma a la que pertenece este ROM.
     *
     * @param plataforma plataforma asociada.
     */
    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    /**
     * Devuelve el título del ROM como representación de texto.
     *
     * @return título del ROM.
     */
    @Override
    public String toString() {
        return titulo;
    }
}
