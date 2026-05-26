package com.proyecta.api_gestion.dto.security;

public record SystemParameterUpsertRequest(
        String key,
        String value,
        String descripcion
) {}
