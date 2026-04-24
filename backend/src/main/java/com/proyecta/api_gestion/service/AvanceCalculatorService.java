package com.proyecta.api_gestion.service;

import java.math.BigDecimal;

public interface AvanceCalculatorService {
    BigDecimal calcularYActualizarAvanceProyecto(String proyectoId);
    BigDecimal calcularYActualizarAvanceFase(Integer faseId);
    BigDecimal calcularYActualizarAvanceHito(Integer hitoId);
}
