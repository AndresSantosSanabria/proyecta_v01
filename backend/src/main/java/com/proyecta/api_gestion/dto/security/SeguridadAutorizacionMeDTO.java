package com.proyecta.api_gestion.dto.security;

import java.util.List;

public record SeguridadAutorizacionMeDTO(
        String username,
        String nombre,
        List<String> roles,
        List<String> permisos,
        boolean administradorLocal,
        boolean transversal,
        List<String> proyectosAsignados
) {}
