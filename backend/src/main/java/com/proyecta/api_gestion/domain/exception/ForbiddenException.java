package com.proyecta.api_gestion.domain.exception;


/**
 * Excepción para errores de autorización (HTTP 403).
 * Se lanza cuando el usuario autenticado no tiene permisos para acceder al recurso.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
