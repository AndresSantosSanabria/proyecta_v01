package com.proyecta.api_gestion.dto.cronograma;

import java.time.LocalDate;
import java.util.List;

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
