package com.proyecta.api_gestion.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * DTO para la información del usuario autenticado.
 */
@Schema(example = """
    {"id":17,"nombre":"María Gómez","correo":"maria.gomez@proyecta.gov.co","rol":"DIRECTOR","rolNombre":"Director de Proyecto","nivelAcceso":3,"dependencia":"Secretaría de Transformación Digital","activo":true,"ultimoAcceso":"2026-07-15T10:30:00","recibirNotificacionesGlobales":true}
    """)
public record UsuarioDTO(
    Integer id,
    String nombre,
    String correo,
    String rol,
    String rolNombre,
    Integer nivelAcceso,
    String dependencia,
    Boolean activo,
    LocalDateTime ultimoAcceso,
    Boolean recibirNotificacionesGlobales
) {}
