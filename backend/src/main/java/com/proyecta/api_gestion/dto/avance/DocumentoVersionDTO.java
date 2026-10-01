package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(example = """
    {
      "id": 5001,
      "entregableId": 101,
      "numeroVersion": 2,
      "nombreArchivo": "evidencia_alcance.pdf",
      "estado": "ACTUAL",
      "mimeType": "application/pdf",
      "sizeBytes": 245760,
      "checksumSha256": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
      "fechaEntrega": "2026-07-15",
      "subidoPor": "luis.coordinador@proyecta.gov.co",
      "subidoRol": "COORDINADOR",
      "subidoEn": "2026-07-15T10:30:00",
      "comentarioCarga": "Corrección de observaciones del revisor",
      "actual": true,
      "descargaUrl": "/api/v1/documentos/versiones/5001/descarga"
    }
    """)
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
