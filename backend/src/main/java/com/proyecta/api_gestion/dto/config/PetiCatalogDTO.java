package com.proyecta.api_gestion.dto.config;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(example = """
    {"vigencias":["2025","2026"],"estrategias":[{"value":"TECNOLOGIAS_INFORMACION","label":"Tecnologías de la Información"},{"value":"GOBIERNO_DIGITAL","label":"Gobierno Digital"}],"furagPreguntas":[{"key":"FURAG_01","label":"¿El proyecto está alineado con el Plan Estratégico Institucional?"}]}
    """)
public record PetiCatalogDTO(
        List<String> vigencias,
        List<CatalogOptionDTO> estrategias,
        List<FuragPreguntaDTO> furagPreguntas
) {
}
