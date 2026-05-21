package com.proyecta.api_gestion.dto.proyecto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableResponseDTO(
    Integer id,
    String nombre,
    BigDecimal ponderacion,
    String estado,
    Boolean conforme,
    LocalDate fechaLimite
) {}
