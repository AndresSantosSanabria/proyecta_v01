package com.proyecta.api_gestion.dto.dashboard;

import java.math.BigDecimal;

/**
 * DTO para la agrupación de proyectos por dependencia en el dashboard.
 */
public record ProjectsByDependenciaDTO(
    String dependencia,
    long cantidadProyectos,
    Double avancePromedio
) {}
