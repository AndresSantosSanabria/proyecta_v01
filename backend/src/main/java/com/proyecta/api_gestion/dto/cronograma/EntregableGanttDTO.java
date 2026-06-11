package com.proyecta.api_gestion.dto.cronograma;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableGanttDTO(
    Integer entregableId,
    String nombre,
    BigDecimal avance,
    BigDecimal ponderacion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String estado
) {}
