package com.proyecta.api_gestion.dto.avance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentoVersionDTO(
        Long id,
        Integer entregableId,
        Integer numeroVersion,
        String nombreArchivo,
        String estado,
        String mimeType,
        Long sizeBytes,
        String checksumSha256,
        LocalDate fechaEntrega,
        String subidoPor,
        String subidoRol,
        LocalDateTime subidoEn,
        String comentarioCarga,
        Boolean actual,
        String descargaUrl
) {
}
