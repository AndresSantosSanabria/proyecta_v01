package com.proyecta.api_gestion.dto.config;

import com.proyecta.api_gestion.domain.model.enums.RespuestaFurag;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"key":"FURAG_01","label":"¿El proyecto está alineado con el Plan Estratégico Institucional?","response":"SI"}
    """)
public record FuragPreguntaRespuestaDTO(
        String key,
        String label,
        RespuestaFurag response
) {
}
