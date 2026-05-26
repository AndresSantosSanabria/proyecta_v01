package com.proyecta.api_gestion.dto.security;

public record SeguridadUsuarioUpdateRequest(
        String username,
        String nombre,
        String correo,
        String dependencia,
        Boolean activo,
        String keycloakSub
) {}
