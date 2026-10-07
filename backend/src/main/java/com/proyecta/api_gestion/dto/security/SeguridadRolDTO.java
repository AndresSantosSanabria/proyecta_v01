package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"id":4,"codigo":"DIRECTOR","nombre":"Director de Proyecto","descripcion":"Rol con responsabilidad sobre la dirección de proyectos asignados","transversal":false,"activo":true,"permisos":[{"id":31,"codigo":"PROYECTO_VER","nombre":"Consultar proyectos","descripcion":"Permite consultar proyectos asignados","activo":true}]}
    """)
public record SeguridadRolDTO(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Boolean transversal,
        Boolean activo,
        List<SeguridadPermisoDTO> permisos
) {}
