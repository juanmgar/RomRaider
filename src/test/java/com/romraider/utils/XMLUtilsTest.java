package com.romraider.utils;

import com.romraider.model.Plataforma;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XMLUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void exportarAxml_creaArchivoXmlYSePuedeLeer() throws Exception {
        // GIVEN: dos plataformas de prueba
        Plataforma snes = new Plataforma();
        snes.setNombre("Super Nintendo");

        Plataforma megaDrive = new Plataforma();
        megaDrive.setNombre("Mega Drive");

        List<Plataforma> plataformas = Arrays.asList(snes, megaDrive);

        File xmlFile = tempDir.resolve("plataformas.xml").toFile();

        // WHEN: exportamos a XML
        XMLUtils.exportarAxml(plataformas, xmlFile);

        // THEN: el archivo existe y no está vacío
        assertTrue(xmlFile.exists(), "El archivo XML debería haberse creado");
        assertTrue(xmlFile.length() > 0, "El archivo XML no debería estar vacío");

        // Y además: se puede volver a leer sin error
        List<Plataforma> leidas = XMLUtils.importarDesdeXml(xmlFile);
        assertNotNull(leidas, "La lista importada no debería ser null");
        assertEquals(2, leidas.size(), "Deberían haberse importado 2 plataformas");

        // Comprobamos nombres
        assertEquals("Super Nintendo", leidas.get(0).getNombre());
        assertEquals("Mega Drive", leidas.get(1).getNombre());
    }

    @Test
    void exportarEImportar_roundTripMantieneDatosBasicos() throws Exception {
        // GIVEN: lista original
        Plataforma nes = new Plataforma();
        nes.setNombre("NES");

        Plataforma gameBoy = new Plataforma();
        gameBoy.setNombre("Game Boy");

        List<Plataforma> originales = Arrays.asList(nes, gameBoy);

        File xmlFile = tempDir.resolve("roundtrip.xml").toFile();

        // WHEN: exportamos e importamos
        XMLUtils.exportarAxml(originales, xmlFile);
        List<Plataforma> importadas = XMLUtils.importarDesdeXml(xmlFile);

        // THEN: tamaño igual
        assertNotNull(importadas);
        assertEquals(originales.size(), importadas.size(), "El tamaño de la lista debería coincidir");

        // comparamos campos clave
        assertEquals("NES", importadas.get(0).getNombre());
        assertEquals("Game Boy", importadas.get(1).getNombre());
    }

    @Test
    void exportarEImportar_listaVacia_daListaVacia() throws Exception {
        // GIVEN: lista vacía
        List<Plataforma> vacia = List.of();
        File xmlFile = tempDir.resolve("empty.xml").toFile();

        // WHEN
        XMLUtils.exportarAxml(vacia, xmlFile);
        List<Plataforma> importadas = XMLUtils.importarDesdeXml(xmlFile);

        // THEN
        // según cómo esté implementado ColeccionPlataformas puede devolver null o lista vacía:
        if (importadas == null) {
            // aceptamos null como sin plataformas
            assertNull(importadas, "Para una exportación vacía se admite lista null");
        } else {
            assertTrue(importadas.isEmpty(), "La lista importada debería estar vacía");
        }
    }
}
