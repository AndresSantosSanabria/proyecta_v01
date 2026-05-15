package com.proyecta.api_gestion.dto.cronograma;

import java.math.BigDecimal;
import java.util.List;

public record FaseGanttDTO(
    Integer faseId,
    String nombre,
    BigDecimal avance,
    List<HitoGanttDTO> hitos
) {}
