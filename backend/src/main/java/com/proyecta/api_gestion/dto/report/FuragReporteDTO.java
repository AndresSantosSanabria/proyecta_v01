package com.proyecta.api_gestion.dto.report;

public record FuragReporteDTO(
    String proyectoId,
    String nombre,
    Boolean esPeti,
    String estrategiaPeti,
    String vigenciaPeti,
    String objetivoGeneral
) {}
