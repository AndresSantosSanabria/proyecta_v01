package com.proyecta.api_gestion.dto.report;

import java.time.LocalDate;

public record EntregablePendienteDTO(
    Integer id,
    String nombre,
    LocalDate fechaEntrega,
    String faseNombre
) {}
