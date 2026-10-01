package com.proyecta.api_gestion.infrastructure;

/**
 * CWE-117: evita que valores provenientes de peticiones (IP, URI, username,
 * templates) inyecten lineas falsas en los archivos de log (log forging)
 * y limita el volumen registrado.
 */
public final class LogSanitizer {

    private static final int MAX_LENGTH = 200;

    private LogSanitizer() {
    }

    public static String clean(String value) {
        if (value == null) {
            return "-";
        }
        String limpio = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        if (limpio.isEmpty()) {
            return "-";
        }
        return limpio.length() <= MAX_LENGTH ? limpio : limpio.substring(0, MAX_LENGTH) + "...";
    }

    public static String clean(Object value) {
        return value == null ? "-" : clean(String.valueOf(value));
    }
}
