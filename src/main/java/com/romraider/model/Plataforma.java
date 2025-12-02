package com.romraider.model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una plataforma/emulador en el sistema.
 *
 * <p>Cada plataforma contiene:</p>
 * <ul>
 *     <li>Un nombre único.</li>
 *     <li>La extensión asociada a los ROMs (ej: .nes, .gba...).</li>
 *     <li>Una carpeta donde se almacenan los ROMs de dicha plataforma.</li>
 *     <li>Listado de ROMs asociados.</li>
 * </ul>
 *
 * <p>Además, está configurada para poder exportarse a XML sin incluir el ID,
 * lo cual evita fugas de detalles internos y conflictos al importar.</p>
 */
@Entity
@Table(name = "plataformas")
@XmlRootElement(name = "plataforma")
@XmlAccessorType(XmlAccessType.FIELD)
public class Plataforma {

    /**
     * Identificador interno en base de datos.
     *
     * <p>Marcado como {@link XmlTransient} para que no forme parte
     * de la exportación/importación XML.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @XmlTransient
    private int id;

    /**
     * Nombre único de la plataforma (ej: "NES", "Game Boy Advance").
     */
    @Column(nullable = false, unique = true)
    private String nombre;

    /**
     * Extensión asociada a los ROMs de esta plataforma
     * (por ejemplo: ".nes", ".gba", ".sfc"...).
     */
    @Column(name = "extension_rom", nullable = false)
    private String extensionRom;

    /**
     * Ruta de la carpeta donde se almacenan los ROMs de esta plataforma.
     */
    @Column(nullable = false)
    private String carpeta;

    /**
     * Relación bidireccional entre plataforma y sus ROMs.
     *
     * <p>En XML se usa un wrapper {@code <roms>} que contiene varios
     * elementos {@code <rom>}.</p>
     *
     * <p>En JPA, {@code orphanRemoval=true} asegura que los ROMs
     * huérfanos se eliminen automáticamente cuando se retiren
     * de la colección.</p>
     */
    @OneToMany(mappedBy = "plataforma", cascade = CascadeType.ALL, orphanRemoval = true)
    @XmlElementWrapper(name = "roms")
    @XmlElement(name = "rom")
    private List<Rom> roms = new ArrayList<>();

    /**
     * Constructor por defecto requerido por JPA y JAXB.
     */
    public Plataforma() {
    }

    /**
     * Crea una plataforma sin ID explícito (útil para nuevas entidades).
     *
     * @param nombre       nombre de la plataforma.
     * @param extensionRom extensión de los ROMs asociados.
     * @param carpeta      carpeta donde se almacenan los ROMs.
     */
    public Plataforma(String nombre, String extensionRom, String carpeta) {
        this.nombre = nombre;
        this.extensionRom = extensionRom;
        this.carpeta = carpeta;
    }

    /**
     * Crea una plataforma con un ID concreto.
     *
     * @param id           identificador interno.
     * @param nombre       nombre de la plataforma.
     * @param extensionRom extensión de los ROMs asociados.
     * @param carpeta      carpeta donde se almacenan los ROMs.
     */
    public Plataforma(int id, String nombre, String extensionRom, String carpeta) {
        this.id = id;
        this.nombre = nombre;
        this.extensionRom = extensionRom;
        this.carpeta = carpeta;
    }

    /**
     * @return identificador interno de la plataforma.
     */
    public int getId() {
        return id;
    }

    /**
     * @return nombre de la plataforma.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @return extensión de archivo asociada a los ROMs.
     */
    public String getExtensionRom() {
        return extensionRom;
    }

    /**
     * @return ruta de la carpeta de ROMs.
     */
    public String getCarpeta() {
        return carpeta;
    }

    /**
     * @return lista de ROMs asociados a la plataforma.
     */
    public List<Rom> getRoms() {
        return roms;
    }

    /**
     * Establece el identificador interno.
     *
     * @param id nuevo identificador.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Establece el nombre de la plataforma.
     *
     * @param nombre nuevo nombre.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Establece la extensión por defecto de los ROMs.
     *
     * @param extensionRom nueva extensión.
     */
    public void setExtensionRom(String extensionRom) {
        this.extensionRom = extensionRom;
    }

    /**
     * Establece la carpeta donde se almacenan los ROMs.
     *
     * @param carpeta nueva ruta de carpeta.
     */
    public void setCarpeta(String carpeta) {
        this.carpeta = carpeta;
    }

    /**
     * Reemplaza la lista completa de ROMs asociados.
     *
     * @param roms nueva colección de ROMs.
     */
    public void setRoms(List<Rom> roms) {
        this.roms = roms;
    }

    /**
     * Devuelve el nombre de la plataforma como representación de texto.
     *
     * @return nombre de la plataforma.
     */
    @Override
    public String toString() {
        return nombre;
    }
}
