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
 */
public class XMLUtils {

    /**
     * Exporta una lista de plataformas (y sus ROMs) a un archivo XML.
     *
     * @param plataformas lista de plataformas a exportar
     * @param archivo archivo donde guardar el XML
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
     * Importa una colección de plataformas desde un archivo XML.
     *
     * @param archivo archivo XML a leer
     * @return lista de plataformas contenida en el archivo
     */
    public static List<Plataforma> importarDesdeXml(File archivo) throws Exception {
        JAXBContext context = JAXBContext.newInstance(ColeccionPlataformas.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        ColeccionPlataformas coleccion =
                (ColeccionPlataformas) unmarshaller.unmarshal(archivo);

        return coleccion.getPlataformas();
    }
}
