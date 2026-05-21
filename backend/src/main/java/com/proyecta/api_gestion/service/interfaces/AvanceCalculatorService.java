package com.proyecta.api_gestion.service.interfaces;

import java.math.BigDecimal;

public interface AvanceCalculatorService extends IProgressCalculator {
    BigDecimal calcularYActualizarAvanceProyecto(String proyectoId);
    BigDecimal calcularYActualizarAvanceFase(Integer faseId);
    BigDecimal calcularYActualizarAvanceHito(Integer hitoId);
}
