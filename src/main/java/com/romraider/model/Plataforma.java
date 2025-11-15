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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @XmlTransient
    private int id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(name = "extension_rom", nullable = false)
    private String extensionRom;

    @Column(nullable = false)
    private String carpeta;

    /**
     * Relación bidireccional entre plataforma y sus ROMs.
     *
     * <p>XML usa un wrapper <roms>, mientras que JPA controla la
     * relación mediante orphanRemoval=true para limpiar ROMs huérfanos.</p>
     */
    @OneToMany(mappedBy = "plataforma", cascade = CascadeType.ALL, orphanRemoval = true)
    @XmlElementWrapper(name = "roms")
    @XmlElement(name = "rom")
    private List<Rom> roms = new ArrayList<>();

    public Plataforma() {
    }

    public Plataforma(String nombre, String extensionRom, String carpeta) {
        this.nombre = nombre;
        this.extensionRom = extensionRom;
        this.carpeta = carpeta;
    }

    public Plataforma(int id, String nombre, String extensionRom, String carpeta) {
        this.id = id;
        this.nombre = nombre;
        this.extensionRom = extensionRom;
        this.carpeta = carpeta;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getExtensionRom() {
        return extensionRom;
    }

    public String getCarpeta() {
        return carpeta;
    }

    public List<Rom> getRoms() {
        return roms;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setExtensionRom(String extensionRom) {
        this.extensionRom = extensionRom;
    }

    public void setCarpeta(String carpeta) {
        this.carpeta = carpeta;
    }

    public void setRoms(List<Rom> roms) {
        this.roms = roms;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
