package com.proyecta.api_gestion.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"value":"TECNOLOGIAS_INFORMACION","label":"Tecnologías de la Información"}
    """)
public record CatalogOptionDTO(
        String value,
        String label
) {
}
