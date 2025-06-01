package com.romraider.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class PropertyUtils {

    private final Properties properties = new Properties();
    private final String filePath;

    public PropertyUtils(String filePath) throws IOException {
        this.filePath = filePath;
        try (InputStream input = new FileInputStream(filePath)) {
            properties.load(input);
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public void save(String comments) throws IOException {
        try (OutputStream out = new FileOutputStream(filePath)) {
            properties.store(out, comments);
        }
    }

    public Properties getProperties() {
        return properties;
    }
}