package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {
      "id": 9001,
      "entregableId": 101,
      "versionId": 5001,
      "numeroVersion": 2,
      "observacion": "La evidencia no incluye el acta de conformidad firmada",
      "estado": "ABIERTA",
      "creadaPor": "ana.revision@proyecta.gov.co",
      "creadaRol": "REVISOR",
      "creadaEn": "2026-07-15T10:30:00",
      "subsanadaPor": null,
      "subsanadaEn": null,
      "comentarioSubsanacion": null,
      "cerradaPor": null,
      "cerradaEn": null
    }
    """)
public record DocumentoObservacionDTO(
        Long id,
        Integer entregableId,
        Long versionId,
        Integer numeroVersion,
        String observacion,
        String estado,
        String creadaPor,
        String creadaRol,
        LocalDateTime creadaEn,
        String subsanadaPor,
        LocalDateTime subsanadaEn,
        String comentarioSubsanacion,
        String cerradaPor,
        LocalDateTime cerradaEn
) {
}
