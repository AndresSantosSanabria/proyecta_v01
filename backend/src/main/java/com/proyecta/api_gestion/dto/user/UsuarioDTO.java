package com.proyecta.api_gestion.dto.user;

/**
 * DTO para la información del usuario autenticado.
 */
public record UsuarioDTO(
    String nombre,
    String correo,
    String rol,
    String dependencia
) {}
