package com.proyecta.api_gestion.dto.cronograma;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
    {"hitoId":45,"nombre":"Levantamiento de información","avance":75.00,"fechaInicio":"2026-04-01","fechaFin":"2026-04-30","entregables":[]}
    """)
public record HitoGanttDTO(
    Integer hitoId,
    String nombre,
    BigDecimal avance,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    List<EntregableGanttDTO> entregables
) {}
