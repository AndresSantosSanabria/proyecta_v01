package com.proyecta.api_gestion.dto.proyecto;

import java.math.BigDecimal;
import java.util.List;

public record HitoResponseDTO(
    Integer id,
    String nombre,
    String descripcion,
    BigDecimal ponderacion,
    BigDecimal avanceCalculado,
    List<EntregableResponseDTO> entregables
) {}
