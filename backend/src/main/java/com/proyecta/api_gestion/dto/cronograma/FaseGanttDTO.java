package com.proyecta.api_gestion.dto.cronograma;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(example = """
    {"faseId":12,"nombre":"Planificación","avance":95.00,"hitos":[]}
    """)
public record FaseGanttDTO(
    Integer faseId,
    String nombre,
    BigDecimal avance,
    List<HitoGanttDTO> hitos
) {}
