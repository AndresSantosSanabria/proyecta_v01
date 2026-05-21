package com.proyecta.api_gestion.dto.proyecto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    long totalEntregables,
    @JsonProperty("puede_cerrar") boolean puedeCerrar,
    List<String> entregables
) {}
