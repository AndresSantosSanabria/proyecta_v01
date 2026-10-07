package com.proyecta.api_gestion.service.support;

import com.proyecta.api_gestion.domain.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Validacion de PDFs compartida por los servicios que adjuntan archivos.
 */
public final class PdfFileSupport {

    private PdfFileSupport() {
    }

    public static void validarPdf(MultipartFile archivo, String mensajeVacio,
                                  String mensajeTipo, String mensajeFirma) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException(mensajeVacio);
        }
        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BadRequestException(mensajeTipo);
        }
        try (var is = archivo.getInputStream()) {
            byte[] encabezado = is.readNBytes(5);
            String firma = new String(encabezado, StandardCharsets.US_ASCII);
            if (!firma.startsWith("%PDF-")) {
                throw new BadRequestException(mensajeFirma);
            }
        } catch (IOException _) {
            throw new BadRequestException("No fue posible validar el archivo PDF cargado.");
        }
    }
}
