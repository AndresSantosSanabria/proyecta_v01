package com.proyecta.api_gestion.dto.avance;

import java.math.BigDecimal;
import java.util.List;

public record FaseAvanceDTO(
    Integer id,
    String nombre,
    BigDecimal ponderacion,
    BigDecimal avance,
    List<HitoAvanceDTO> hitos
) {}
