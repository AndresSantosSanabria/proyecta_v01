package com.proyecta.api_gestion.dto.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableResponseDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    String estado,
    Boolean conforme,
    LocalDate fechaInicio,
    LocalDate fechaLimite
) {}
