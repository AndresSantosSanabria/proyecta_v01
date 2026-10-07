package com.proyecta.api_gestion.domain.exception;


/**
 * Excepción para errores de validación de datos de entrada (HTTP 400).
 * Se lanza cuando los parámetros o datos enviados por el cliente son inválidos.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
