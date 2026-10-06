package com.proyecta.api_gestion.domain.value;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Contenido de archivo que cruza los puertos de entrada (ADR-007 dec.4).
 * Reemplaza a org.springframework.web.multipart.MultipartFile fuera del adaptador web.
 * El flujo se abre bajo demanda y lo cierra quien lo consume.
 */
public record FileUpload(String originalFilename, String contentType, long size,
                         Supplier<InputStream> content) {
}
