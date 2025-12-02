package com.romraider.utils;

import com.romraider.model.ColeccionPlataformas;
import com.romraider.model.Plataforma;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.List;

/**
 * Utilidades para importar y exportar plataformas y ROMs
 * usando JAXB en formato XML.
 * <p>
 * Esta clase encapsula la conversión entre objetos del dominio
 * ({@link Plataforma}, {@link ColeccionPlataformas}) y su representación
 * estructurada en XML, permitiendo persistencia e intercambio sencillo
 * de colecciones completas.
 */
public class XMLUtils {

    /**
     * Exporta una lista de plataformas (y sus ROMs asociadas) a un archivo XML.
     * <p>
     * Internamente, la lista se envuelve en un contenedor {@link ColeccionPlataformas}
     * requerido por JAXB para la serialización.
     * <p>
     * El archivo generado se escribe de forma legible gracias a la propiedad
     * {@link Marshaller#JAXB_FORMATTED_OUTPUT}.
     *
     * @param plataformas lista de plataformas a exportar.
     * @param archivo     archivo destino donde guardar el contenido XML.
     * @throws Exception si ocurre un error durante la creación del contexto JAXB
     *                   o en el proceso de escritura del archivo.
     */
    public static void exportarAxml(List<Plataforma> plataformas, File archivo) throws Exception {
        ColeccionPlataformas coleccion = new ColeccionPlataformas();
        coleccion.setPlataformas(plataformas);

        JAXBContext context = JAXBContext.newInstance(ColeccionPlataformas.class);
        Marshaller marshaller = context.createMarshaller();

        // Salida legible
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        marshaller.marshal(coleccion, archivo);
    }

    /**
     * Importa una colección de plataformas desde un archivo XML previamente
     * exportado.
     * <p>
     * El XML debe respetar la estructura generada por
     * {@link #exportarAxml(List, File)}.
     *
     * @param archivo archivo XML a leer.
     * @return lista de plataformas contenida en el archivo.
     * @throws Exception si ocurre un error de lectura o si el archivo no es válido.
     */
    public static List<Plataforma> importarDesdeXml(File archivo) throws Exception {
        JAXBContext context = JAXBContext.newInstance(ColeccionPlataformas.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        ColeccionPlataformas coleccion =
                (ColeccionPlataformas) unmarshaller.unmarshal(archivo);

        return coleccion.getPlataformas();
    }
}
