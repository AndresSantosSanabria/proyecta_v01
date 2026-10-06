package com.proyecta.api_gestion.domain.exception;


public class UnprocessableEntityException extends RuntimeException {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
