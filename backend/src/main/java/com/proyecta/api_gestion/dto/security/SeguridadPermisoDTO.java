package com.proyecta.api_gestion.dto.security;

public record SeguridadPermisoDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo
) {}
