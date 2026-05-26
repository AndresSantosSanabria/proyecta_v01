package com.proyecta.api_gestion.dto.security;

public record SystemParameterDTO(
        String key,
        String value,
        String descripcion
) {}
