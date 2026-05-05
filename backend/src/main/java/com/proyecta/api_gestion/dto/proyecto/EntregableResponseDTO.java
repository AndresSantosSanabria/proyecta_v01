package com.proyecta.api_gestion.dto.proyecto;

import com.proyecta.api_gestion.model.enums.EstadoEntregable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EntregableResponseDTO(
    Integer id,
    String nombre,
    BigDecimal ponderacion,
    EstadoEntregable estado,
    Boolean conforme,
    LocalDate fechaLimite
) {}
