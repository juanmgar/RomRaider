package pruebas;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Prueba {

    // Patrón corregido: sin .* antes de la extensión
    private static final Pattern romPattern =
            Pattern.compile("^(.*?)\\s*(?:[\\[(]([^\\]\\)]+)[\\])])?\\.(\\w+)$");

    public static void main(String[] args) {
        File baseDir = new File("roms-de-prueba");
        scanDirectory(baseDir);
    }

    private static void scanDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Directorio inválido: " + dir.getAbsolutePath());
            return;
        }

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else {
                parseRomFilename(file);
            }
        }
    }

    private static void parseRomFilename(File file) {
        String name = file.getName();
        Matcher matcher = romPattern.matcher(name);
        if (matcher.matches()) {
            String title = matcher.group(1) != null ? matcher.group(1).trim() : "Desconocido";
            String region = matcher.group(2) != null ? matcher.group(2).trim() : "Desconocida";
            String extension = matcher.group(3).trim().toLowerCase();
            String descripcion = RawgAPI.buscarDescripcionJuego(title, extension);

            // Limpieza opcional del título (puntos, guiones, guiones bajos → espacio)
            title = title.replaceAll("[._-]", " ").replaceAll("\\s{2,}", " ").trim();

            // Inserta espacio entre dígito y letra (ej: 007Agente → 007 Agente)
            title = title.replaceAll("(?<=\\d)(?=\\p{L})", " ");

            // Inserta espacio entre letra y dígito (ej: MarioKart64 → MarioKart 64)
            title = title.replaceAll("(?<=\\p{L})(?=\\d)", " ");

            String platform = detectPlatform(extension);

            System.out.println("Archivo: " + name);
            System.out.println("  Título: " + title);
            System.out.println("  Región: " + region);
            System.out.println("  Plataforma: " + platform);
            System.out.println("  Descripción: " + (descripcion != null ? descripcion : "No encontrada"));

            System.out.println();
        } else {
            System.out.println("No coincide patrón: " + name);
        }
    }

    private static String detectPlatform(String extension) {
        return switch (extension) {
            case "nes" -> "Nintendo Entertainment System";
            case "sfc", "smc" -> "Super Nintendo";
            case "gba" -> "Game Boy Advance";
            case "gb" -> "Game Boy";
            case "gbc" -> "Game Boy Color";
            case "n64", "z64" -> "Nintendo 64";
            case "gen", "md" -> "Sega Mega Drive / Genesis";
            case "sms" -> "Sega Master System";
            default -> "Desconocida";
        };
    }
}
