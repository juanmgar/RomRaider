package com.romraider.utils;

import com.romraider.model.ColeccionPlataformas;
import com.romraider.model.Plataforma;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.List;

public class XMLUtils {

    public static void exportarAxml(List<Plataforma> plataformas, File archivo) throws Exception {
        ColeccionPlataformas coleccion = new ColeccionPlataformas();
        coleccion.setPlataformas(plataformas);

        JAXBContext context = JAXBContext.newInstance(ColeccionPlataformas.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.marshal(coleccion, archivo);
    }

    public static List<Plataforma> importarDesdeXml(File archivo) throws Exception {
        JAXBContext context = JAXBContext.newInstance(ColeccionPlataformas.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ColeccionPlataformas coleccion = (ColeccionPlataformas) unmarshaller.unmarshal(archivo);
        return coleccion.getPlataformas();
    }
}
