package com.proyecta.api_gestion.dto.risk;

import java.util.List;

public record RiesgoListResponseDTO(
    String proyectoId,
    String proyectoNombre,
    List<RiesgoResponseDTO> riesgos
) {}
