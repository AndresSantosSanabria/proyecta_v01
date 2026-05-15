package com.proyecta.api_gestion.dto.cronograma;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HitoGanttDTO(
    Integer hitoId,
    String nombre,
    BigDecimal avance,
    LocalDate fechaInicio,
    LocalDate fechaFin
) {}
