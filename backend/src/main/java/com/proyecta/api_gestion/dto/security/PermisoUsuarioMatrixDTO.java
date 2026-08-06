package com.proyecta.api_gestion.dto.security;

import java.util.List;

public record PermisoUsuarioMatrixDTO(
        Long usuarioId,
        String username,
        String nombre,
        String rolCodigo,
        List<PermisoItemDTO> permisos
) {
    public record PermisoItemDTO(
            Long permisoId,
            String codigo,
            String nombre,
            String categoria,
            boolean concedido,
            boolean source,
            boolean sidebar
    ) {}
}
