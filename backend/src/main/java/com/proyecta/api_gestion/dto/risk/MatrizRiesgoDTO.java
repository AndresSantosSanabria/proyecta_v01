package com.proyecta.api_gestion.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"probabilidad":"ALTA","impacto":"ALTA","nivel":"EXTREMO","color":"#D00000"}
    """)
public record MatrizRiesgoDTO(
        String probabilidad,
        String impacto,
        String nivel,
        String color
) {}
