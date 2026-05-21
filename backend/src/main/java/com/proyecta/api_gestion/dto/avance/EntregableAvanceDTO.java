package com.proyecta.api_gestion.dto.avance;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableAvanceDTO(
    Integer id,
    String nombre,
    BigDecimal ponderacion,
    LocalDate fechaLimite,
    BigDecimal avance,
    String estado,
    Long atraso,
    LocalDate fechaEntrega,
    String evidenciaPdf,
    String evidenciaUrl
) {}
