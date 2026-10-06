package com.proyecta.api_gestion.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para la agrupación de proyectos por dependencia en el dashboard.
 */
@Schema(example = """
    {"dependencia":"Secretaría de Transformación Digital","cantidadProyectos":12,"avancePromedio":72.4}
    """)
public record ProjectsByDependenciaDTO(
    String dependencia,
    long cantidadProyectos,
    Double avancePromedio
) {}
