package com.romraider.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class PropertyUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void constructor_loadsExistingPropertiesFile() throws IOException {
        // given: un fichero de propiedades en el disco
        Path propsFile = tempDir.resolve("config.properties");
        String content = """
                romraider.roms.default-folder=roms
                app.mode=prod
                """;
        Files.writeString(propsFile, content);

        // when
        PropertyUtils propertyUtils = new PropertyUtils(propsFile.toString());

        // then
        assertEquals("roms", propertyUtils.get("romraider.roms.default-folder"));
        assertEquals("prod", propertyUtils.get("app.mode"));
        assertEquals(2, propertyUtils.getProperties().size());
    }

    @Test
    void constructor_nonExistingFile_throwsIOException() {
        // given
        Path propsFile = tempDir.resolve("does_not_exist.properties");

        // when + then
        assertThrows(IOException.class, () -> new PropertyUtils(propsFile.toString()));
    }

    @Test
    void get_returnsNullWhenKeyDoesNotExist() throws IOException {
        // given
        Path propsFile = tempDir.resolve("empty.properties");
        Files.writeString(propsFile, ""); // fichero vacío

        PropertyUtils propertyUtils = new PropertyUtils(propsFile.toString());

        // when
        String value = propertyUtils.get("non.existing.key");

        // then
        assertNull(value);
    }

    @Test
    void getOrDefault_returnsDefaultWhenKeyMissing() throws IOException {
        // given
        Path propsFile = tempDir.resolve("config.properties");
        Files.writeString(propsFile, "some.key=someValue\n");

        PropertyUtils propertyUtils = new PropertyUtils(propsFile.toString());

        // when
        String value = propertyUtils.getOrDefault("other.key", "defaultValue");

        // then
        assertEquals("defaultValue", value);
    }

    @Test
    void set_updatesPropertyInMemory() throws IOException {
        // given
        Path propsFile = tempDir.resolve("config.properties");
        Files.writeString(propsFile, "key1=value1\n");
        PropertyUtils propertyUtils = new PropertyUtils(propsFile.toString());

        // when
        propertyUtils.set("key1", "newValue");
        propertyUtils.set("key2", "anotherValue");

        // then
        assertEquals("newValue", propertyUtils.get("key1"));
        assertEquals("anotherValue", propertyUtils.get("key2"));
    }

    @Test
    void save_persistsChangesToDisk() throws IOException {
        // given: fichero inicial
        Path propsFile = tempDir.resolve("config.properties");
        Files.writeString(propsFile, "key1=value1\n");
        PropertyUtils propertyUtils = new PropertyUtils(propsFile.toString());

        // when: modificamos en memoria y guardamos
        propertyUtils.set("key1", "changed");
        propertyUtils.set("new.key", "new");
        propertyUtils.save("Test save");

        // then: volvemos a leer el fichero directamente con Properties
        Properties reloaded = new Properties();
        try (var in = Files.newInputStream(propsFile)) {
            reloaded.load(in);
        }

        assertEquals("changed", reloaded.getProperty("key1"));
        assertEquals("new", reloaded.getProperty("new.key"));
    }
}
