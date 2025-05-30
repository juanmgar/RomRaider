package com.romraider.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plataformas")
public class Plataforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(name = "extension_rom", nullable = false)
    private String extensionRom;

    @Column(nullable = false)
    private String ruta;

    @OneToMany(mappedBy = "plataforma", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rom> roms = new ArrayList<>();

    public Plataforma() {
    }

    public Plataforma(String nombre, String extensionRom, String ruta) {
        this.nombre = nombre;
        this.extensionRom = extensionRom;
        this.ruta = ruta;
    }

    public Plataforma(int id, String nombre, String extensionRom, String ruta) {
        this.id = id;
        this.nombre = nombre;
        this.extensionRom = extensionRom;
        this.ruta = ruta;
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

    public String getRuta() {
        return ruta;
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

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public void setRoms(List<Rom> roms) {
        this.roms = roms;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
