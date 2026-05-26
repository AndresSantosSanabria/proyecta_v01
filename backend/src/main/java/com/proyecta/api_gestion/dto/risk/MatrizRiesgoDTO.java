package com.proyecta.api_gestion.dto.risk;

public record MatrizRiesgoDTO(
        String probabilidad,
        String impacto,
        String nivel,
        String color
) {}
