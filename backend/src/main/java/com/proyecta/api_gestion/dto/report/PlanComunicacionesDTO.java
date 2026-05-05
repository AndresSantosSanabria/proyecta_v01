package com.proyecta.api_gestion.dto.report;

public record PlanComunicacionesDTO(
    String proyectoId,
    String nombre,
    String planPdfUrl,
    String dependencia
) {}
