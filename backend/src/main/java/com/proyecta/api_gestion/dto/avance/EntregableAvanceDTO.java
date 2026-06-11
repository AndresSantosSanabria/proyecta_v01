package com.proyecta.api_gestion.dto.avance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableAvanceDTO(
    Integer id,
    String nombre,
    BigDecimal ponderacion,
    LocalDate fechaInicio,
    LocalDate fechaLimite,
    BigDecimal progresoProgramado,
    BigDecimal progresoEjecutado,
    BigDecimal diferencia,
    BigDecimal eficacia,
    String estado,
    Long diasAtraso,
    LocalDate fechaEntrega,
    String evidenciaPdf,
    String evidenciaUrl,
    BigDecimal avance,
    Long atraso,
    String estadoCodigo,
    String observacionRevision
) {}
