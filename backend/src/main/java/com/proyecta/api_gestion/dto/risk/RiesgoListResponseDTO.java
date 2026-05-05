package com.proyecta.api_gestion.dto.risk;

import java.util.List;

public record RiesgoListResponseDTO(
    String proyectoId,
    List<RiesgoResponseDTO> riesgos
) {}
