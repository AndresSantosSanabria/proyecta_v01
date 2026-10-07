package com.proyecta.api_gestion.domain.exception;


/**
 * Excepción para errores de autenticación (HTTP 401).
 * Se lanza cuando el usuario no está autenticado o las credenciales son inválidas.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
