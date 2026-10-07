package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {
      "proyectoId": "PROY-CUN-2026-008",
      "proyectoNombre": "Modernización de Redes LAN",
      "riesgos": []
    }
    """)
public record RiesgoListResponseDTO(
    String proyectoId,
    String proyectoNombre,
    List<RiesgoResponseDTO> riesgos
) {}
