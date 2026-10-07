package com.proyecta.api_gestion.infrastructure;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * CWE-434 / CWE-79: el Content-Type de una carga lo declara el cliente y por lo
 * tanto es manipulable. Aqui el tipo se deriva de la extension (allowlist) y, en
 * ultima instancia, del Content-Type declarado solo si tambien pertenece a la
 * allowlist; cualquier otra cosa se degrada a application/octet-stream para que
 * el navegador nunca interprete el archivo como HTML/JS.
 */
public final class UploadMimeSanitizer {

    private static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String MIME_IMAGE_JPEG = "image/jpeg";

    private static final Map<String, String> EXTENSION_A_MIME = Map.ofEntries(
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", MIME_IMAGE_JPEG),
            Map.entry(".jpeg", MIME_IMAGE_JPEG),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".zip", "application/zip"));

    private static final Set<String> MIME_PERMITIDO = Set.of(
            "application/pdf",
            "image/png",
            MIME_IMAGE_JPEG,
            "image/gif",
            "image/webp",
            "text/plain",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            MIME_APPLICATION_OCTET_STREAM);

    private UploadMimeSanitizer() {
    }

    /**
     * Valida la firma real del archivo (%PDF-), sin confiar en el Content-Type.
     */
    public static boolean esPdfValido(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        try (var in = file.getInputStream()) {
            byte[] header = in.readNBytes(5);
            String firma = new String(header, StandardCharsets.ISO_8859_1);
            return firma.startsWith("%PDF-");
        } catch (IOException _) {
            return false;
        }
    }

    /**
     * Resuelve el MIME seguro a partir de la extension; solo recurre al
     * Content-Type declarado si este esta en la allowlist.
     */
    public static String resolverMimeType(MultipartFile file) {
        if (file == null) {
            return MIME_APPLICATION_OCTET_STREAM;
        }

        String extension = extensionDe(file.getOriginalFilename());
        String porExtension = EXTENSION_A_MIME.get(extension);
        if (porExtension != null) {
            return porExtension;
        }

        String declarado = file.getContentType();
        if (declarado != null && !declarado.isBlank()) {
            String limpio = declarado.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (MIME_PERMITIDO.contains(limpio)) {
                return limpio;
            }
        }

        return MIME_APPLICATION_OCTET_STREAM;
    }

    public static String extensionDe(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
