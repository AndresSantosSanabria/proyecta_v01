package com.proyecta.api_gestion.dto.avance;

import java.math.BigDecimal;
import java.util.List;

public record HitoAvanceDTO(
    Integer id,
    String nombre,
    BigDecimal ponderacion,
    BigDecimal progresoProgramado,
    BigDecimal progresoEjecutado,
    BigDecimal diferencia,
    BigDecimal eficacia,
    String estado,
    BigDecimal avance,
    List<EntregableAvanceDTO> entregables
) {}
