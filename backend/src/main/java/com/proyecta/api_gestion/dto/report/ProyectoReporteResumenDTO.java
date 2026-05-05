package com.proyecta.api_gestion.dto.report;

import java.math.BigDecimal;

public record ProyectoReporteResumenDTO(
    String id,
    String nombre,
    BigDecimal avance,
    String dependencia,
    String estado,
    Long entregablesAtrasados
) {}
