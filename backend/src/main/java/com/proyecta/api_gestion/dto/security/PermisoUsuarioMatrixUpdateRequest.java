package com.proyecta.api_gestion.dto.security;

import java.util.List;

public record PermisoUsuarioMatrixUpdateRequest(
        Long usuarioId,
        List<PermisoUpdateItem> permisos
) {
    public record PermisoUpdateItem(
            Long permisoId,
            boolean concedido
    ) {}
}
