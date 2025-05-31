package com.romraider.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final Path SESSION_FILE = Path.of(System.getProperty("user.home"), ".romraider", "session.json");

    public static void saveSession(String token) {
        try {
            Files.createDirectories(SESSION_FILE.getParent());
            Files.writeString(SESSION_FILE, token);
            logger.info("Token saved to: {}", SESSION_FILE);
        } catch (IOException e) {
            logger.error("Failed to save token to: {}", SESSION_FILE, e);
        }
    }

    public static String loadSession() {
        try {
            if (Files.exists(SESSION_FILE)) {
                String token = Files.readString(SESSION_FILE).trim();
                logger.info("Token loaded from: {}", SESSION_FILE);
                return token;
            } else {
                logger.info("No session file found at: {}", SESSION_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to load token from: {}", SESSION_FILE, e);
        }
        return null;
    }

    public static void clearSession() {
        try {
            if (Files.deleteIfExists(SESSION_FILE)) {
                logger.info("Session file deleted: {}", SESSION_FILE);
            } else {
                logger.info("No session file to delete at: {}", SESSION_FILE);
            }
        } catch (IOException e) {
            logger.error("Failed to delete session file: {}", SESSION_FILE, e);
        }
    }
}
