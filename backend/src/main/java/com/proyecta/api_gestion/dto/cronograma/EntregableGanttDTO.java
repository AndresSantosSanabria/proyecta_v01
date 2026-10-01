package com.proyecta.api_gestion.dto.cronograma;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(example = """
    {"entregableId":101,"nombre":"Documento de alcance aprobado","avance":100.00,"ponderacion":5.00,"fechaInicio":"2026-07-01","fechaFin":"2026-07-20","estado":"APROBADO"}
    """)
public record EntregableGanttDTO(
    Integer entregableId,
    String nombre,
    BigDecimal avance,
    BigDecimal ponderacion,
    LocalDate fechaInicio,
    LocalDate fechaFin,
    String estado
) {}
