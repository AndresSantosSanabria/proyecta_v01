package com.proyecta.api_gestion.dto.avance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndicadoresEficienciaDTO(
    String proyectoId,
    String nombreProyecto,
    LocalDate fechaCorte,
    long programadosAlCorte,
    long entregadosAlCorte,
    long entregadosATiempo,
    BigDecimal eficacia,
    BigDecimal eficiencia,
    long totalEntregables
) {}
