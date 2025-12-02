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
 *     &lt;coleccion&gt;
 *         &lt;plataforma&gt; ... &lt;/plataforma&gt;
 *         &lt;plataforma&gt; ... &lt;/plataforma&gt;
 *     &lt;/coleccion&gt;
 * </pre>
 * </p>
 */
@XmlRootElement(name = "coleccion")
@XmlAccessorType(XmlAccessType.FIELD)
public class ColeccionPlataformas {

    /**
     * Lista de plataformas que forman parte de la colección a exportar/importar.
     *
     * <p>Cada elemento se serializa como un nodo {@code <plataforma>} dentro
     * del elemento raíz {@code <coleccion>}.</p>
     */
    @XmlElement(name = "plataforma")
    private List<Plataforma> plataformas = new ArrayList<>();

    /**
     * Devuelve la lista de plataformas contenida en la colección.
     *
     * @return lista de plataformas (nunca {@code null}, aunque puede estar vacía).
     */
    public List<Plataforma> getPlataformas() {
        return plataformas;
    }

    /**
     * Establece la lista completa de plataformas de la colección.
     *
     * @param plataformas lista de plataformas a asociar al contenedor.
     */
    public void setPlataformas(List<Plataforma> plataformas) {
        this.plataformas = plataformas;
    }
}
