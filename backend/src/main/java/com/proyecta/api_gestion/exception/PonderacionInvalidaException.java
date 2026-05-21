package com.proyecta.api_gestion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.math.BigDecimal;

/**
 * Excepción lanzada cuando las ponderaciones de las fases de un proyecto
 * violan las reglas de negocio (suma != 100% en estado final)
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class PonderacionInvalidaException extends RuntimeException {
    
    private final BigDecimal sumaActual;
    private final String proyectoId;

    public PonderacionInvalidaException(String message) {
        super(message);
        this.sumaActual = null;
        this.proyectoId = null;
    }

    public PonderacionInvalidaException(String message, BigDecimal sumaActual, String proyectoId) {
        super(message);
        this.sumaActual = sumaActual;
        this.proyectoId = proyectoId;
    }

    public BigDecimal getSumaActual() {
        return sumaActual;
    }

    public String getProyectoId() {
        return proyectoId;
    }
}
