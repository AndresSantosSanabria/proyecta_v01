package com.proyecta.api_gestion.dto.user;

import java.time.LocalDateTime;

/**
 * DTO para la información del usuario autenticado.
 */
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
