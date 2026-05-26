package com.proyecta.api_gestion.dto.security;

public record SeguridadUsuarioProyectoRequest(
        String username,
        String proyectoId,
        String cargo
) {}
