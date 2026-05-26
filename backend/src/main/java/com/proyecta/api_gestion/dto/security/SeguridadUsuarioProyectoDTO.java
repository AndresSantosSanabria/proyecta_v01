package com.proyecta.api_gestion.dto.security;

import java.time.LocalDateTime;

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
