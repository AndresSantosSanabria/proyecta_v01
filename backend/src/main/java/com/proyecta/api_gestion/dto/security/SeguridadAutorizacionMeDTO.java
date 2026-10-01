package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"username":"maria.gomez","nombre":"María Gómez","roles":["DIRECTOR"],"permisos":["PROYECTO_VER","RIESGO_VER","RIESGO_EDITAR"],"administradorLocal":false,"transversal":false,"proyectosAsignados":["PROY-CUN-2026-008"]}
    """)
public record SeguridadAutorizacionMeDTO(
        String username,
        String nombre,
        List<String> roles,
        List<String> permisos,
        boolean administradorLocal,
        boolean transversal,
        List<String> proyectosAsignados
) {}
