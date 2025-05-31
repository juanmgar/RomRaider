package com.romraider.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "coleccion")
@XmlAccessorType(XmlAccessType.FIELD)
public class ColeccionPlataformas {

    @XmlElement(name = "plataforma")
    private List<Plataforma> plataformas = new ArrayList<>();

    public List<Plataforma> getPlataformas() {
        return plataformas;
    }

    public void setPlataformas(List<Plataforma> plataformas) {
        this.plataformas = plataformas;
    }
}