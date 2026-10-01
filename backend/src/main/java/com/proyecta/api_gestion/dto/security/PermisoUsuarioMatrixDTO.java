package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"usuarioId":17,"username":"maria.gomez","nombre":"María Gómez","rolCodigo":"DIRECTOR","permisos":[{"permisoId":31,"codigo":"PROYECTO_VER","nombre":"Consultar proyectos","categoria":"PROYECTO","concedido":true,"source":true,"sidebar":true}]}
    """)
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
