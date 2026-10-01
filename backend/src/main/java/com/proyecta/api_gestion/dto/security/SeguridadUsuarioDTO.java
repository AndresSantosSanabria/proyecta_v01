package com.proyecta.api_gestion.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":17,"username":"maria.gomez","nombre":"María Gómez","correo":"maria.gomez@proyecta.gov.co","dependencia":"Secretaría de Transformación Digital","activo":true,"rolCodigo":"DIRECTOR","rolNombre":"Director de Proyecto","fechaCreacion":"2026-01-15T09:00:00","ultimoAcceso":"2026-07-15T10:30:00"}
    """)
public record SeguridadUsuarioDTO(
        Long id,
        String username,
        String nombre,
        String correo,
        String dependencia,
        Boolean activo,
        String rolCodigo,
        String rolNombre,
        LocalDateTime fechaCreacion,
        LocalDateTime ultimoAcceso
) {}
