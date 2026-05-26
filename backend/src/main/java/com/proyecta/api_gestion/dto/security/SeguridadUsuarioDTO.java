package com.proyecta.api_gestion.dto.security;

import java.time.LocalDateTime;

public record SeguridadUsuarioDTO(
        Long id,
        String username,
        String nombre,
        String correo,
        String dependencia,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime ultimoAcceso
) {}
