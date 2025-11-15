package com.romraider.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa el nodo raíz del XML exportado/importado por la aplicación.
 *
 * <p>Este contenedor permite serializar y deserializar una lista completa de
 * plataformas con sus ROMs asociadas mediante JAXB.</p>
 *
 * <p>Ejemplo de estructura esperada:
 * <pre>
 *     <coleccion>
 *         <plataforma> ... </plataforma>
 *         <plataforma> ... </plataforma>
 *     </coleccion>
 * </pre>
 * </p>
 */
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
