package com.proyecta.api_gestion.dto.avance;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(example = """
    {
      "entregableId": 101,
      "estado": "APROBADO",
      "fechaEntrega": "2026-07-15",
      "evidenciaUrl": "/api/v1/evidencias/101/descarga",
      "avanceActualizado": null
    }
    """)
public record EntregableAprobadoResponseDTO(
    Integer entregableId,
    String estado,
    LocalDate fechaEntrega,
    String evidenciaUrl,
    ProyectoAvanceResponseDTO avanceActualizado
) {}
