package com.proyecta.api_gestion.dto.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para el resumen ejecutivo del proyecto previo al cierre.
 * Uso de 'record' siguiendo las recomendaciones de Java 25.
 */
public record ProyectoResumenDTO(
    String id,
    String nombre,
    String director,
    LocalDate fechaInicio,
    BigDecimal avanceTotal,
    String estado,
    long totalFases,
    long totalHitos,
    long entregablesConformes,
    long totalEntregables
) {}
