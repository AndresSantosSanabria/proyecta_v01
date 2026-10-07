package com.proyecta.api_gestion.dto.cronograma;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","nombreArchivo":"cronograma_maestro.xlsx","fechaCarga":"2026-07-15","director":"María Gómez","totalFases":4,"totalHitos":12,"avance":55.00,"fechaInicio":"2026-03-02","descargaUrl":"/api/v1/proyectos/PROY-CUN-2026-008/cronograma/descarga","vistaGantt":[]}
    """)
public record CronogramaResponseDTO(
    String proyectoId,
    String nombreArchivo,
    LocalDate fechaCarga,
    String director,
    Integer totalFases,
    Integer totalHitos,
    java.math.BigDecimal avance,
    LocalDate fechaInicio,
    String descargaUrl,
    List<FaseGanttDTO> vistaGantt
) {}
