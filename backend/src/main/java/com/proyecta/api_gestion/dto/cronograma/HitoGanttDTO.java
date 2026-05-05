package com.proyecta.api_gestion.dto.cronograma;

import java.time.LocalDate;

public record HitoGanttDTO(
    Integer hitoId,
    String nombre,
    LocalDate fechaInicio,
    LocalDate fechaFin
) {}
