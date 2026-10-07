package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":31,"codigo":"PROYECTO_VER","nombre":"Consultar proyectos","descripcion":"Permite consultar proyectos asignados","activo":true}
    """)
public record SeguridadPermisoDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean activo
) {}
