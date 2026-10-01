package com.proyecta.api_gestion.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"key":"FURAG_01","label":"¿El proyecto está alineado con el Plan Estratégico Institucional?"}
    """)
public record FuragPreguntaDTO(
        String key,
        String label
) {
}
