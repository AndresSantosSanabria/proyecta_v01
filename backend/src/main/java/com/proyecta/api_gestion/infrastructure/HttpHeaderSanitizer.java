package com.proyecta.api_gestion.infrastructure;

import java.util.regex.Pattern;

/**
 * Sanitiza valores que viajan en cabeceras HTTP (CWE-113 / CWE-64).
 *
 * Un nombre de archivo o un identificador proveniente del cliente (upload o
 * path variable) jamas debe concatenarse crudo en Content-Disposition: un CR/LF
 * o una comilla permiten inyectar cabeceras falsas o dividir la respuesta.
 *
 * Whitelist estricta: solo se conservan letras, digitos, punto, guion bajo,
 * guion y espacio. Cualquier otro caracter (incluidos \r \n " \ ;) se sustituye
 * por guion bajo. El nombre vacio tras sanear cae en el fallback.
 */
public final class HttpHeaderSanitizer {

    private static final Pattern UNSAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._ -]");
    private static final String FALLBACK = "documento";

    private HttpHeaderSanitizer() {
    }

    /**
     * @param rawName nombre potencialmente no confiable
     * @return nombre seguro para usar dentro de una cabecera
     */
    public static String safeFileName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return FALLBACK;
        }
        String sanitized = UNSAFE_CHARS.matcher(rawName.trim()).replaceAll("_");
        // colapsa guiones bajos consecutivos y recorta a un largo razonable
        sanitized = sanitized.replaceAll("_+", "_");
        if (sanitized.isBlank() || sanitized.matches("[._ -]+")) {
            return FALLBACK;
        }
        return sanitized.length() > 180 ? sanitized.substring(0, 180) : sanitized;
    }

    /**
     * Construye una cabecera Content-Disposition saneada.
     *
     * @param mode "inline" o "attachment"
     * @param rawFileName nombre del archivo (no confiable)
     */
    public static String contentDisposition(String mode, String rawFileName) {
        String safeMode = "attachment".equalsIgnoreCase(mode) ? "attachment" : "inline";
        return safeMode + "; filename=\"" + safeFileName(rawFileName) + "\"";
    }
}
