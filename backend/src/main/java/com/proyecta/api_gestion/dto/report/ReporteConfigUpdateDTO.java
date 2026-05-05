package com.proyecta.api_gestion.dto.report;

public record ReporteConfigUpdateDTO(
    String nombre,
    String descripcion,
    Integer orden,
    Boolean activo
) {}
