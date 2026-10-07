package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":9001,"usuarioId":17,"username":"maria.gomez","nombreUsuario":"María Gómez","proyectoId":"PROY-CUN-2026-008","proyectoCodigo":"PROY-CUN-2026-008","proyectoNombre":"Modernización de Redes LAN","cargo":"Director del proyecto","activo":true,"fechaAsignacion":"2026-03-02T08:00:00"}
    """)
public record SeguridadUsuarioProyectoDTO(
        Long id,
        Long usuarioId,
        String username,
        String nombreUsuario,
        String proyectoId,
        String proyectoCodigo,
        String proyectoNombre,
        String cargo,
        Boolean activo,
        LocalDateTime fechaAsignacion
) {}
