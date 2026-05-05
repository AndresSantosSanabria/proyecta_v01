package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.NivelRiesgo;

public record RiesgoCreatedResponseDTO(
    Integer id,
    String codigo,
    NivelRiesgo nivel,
    String mensaje
) {}
