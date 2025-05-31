package com.romraider.model;

import jakarta.persistence.*;
import jakarta.xml.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
