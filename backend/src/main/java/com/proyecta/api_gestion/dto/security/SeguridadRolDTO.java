package com.proyecta.api_gestion.dto.security;

import java.util.List;

public record SeguridadRolDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean transversal,
        Boolean activo,
        List<SeguridadPermisoDTO> permisos
) {}
