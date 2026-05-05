package com.proyecta.api_gestion.dto.report;

import java.time.LocalDateTime;

public record RiesgoReporteDTO(
    Integer id,
    String codigo,
    String descripcion,
    Integer probabilidad,
    Integer impacto,
    String nivel,
    String tratamiento,
    String estado,
    LocalDateTime fechaActualizacion
) {}
