package com.proyecta.api_gestion.dto.security;

public record SeguridadRolRequest(
        String codigo,
        String nombre,
        String descripcion,
        Boolean transversal,
        Boolean activo
) {}
