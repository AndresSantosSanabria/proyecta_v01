package com.proyecta.api_gestion.dto.risk;

import com.proyecta.api_gestion.model.enums.NivelRiesgo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":34,"codigo":"RIES-PROY-CUN-2026-008-001","nivel":"ALTO","mensaje":"Riesgo registrado correctamente"}
    """)
public record RiesgoCreatedResponseDTO(
    Integer id,
    String codigo,
    NivelRiesgo nivel,
    String mensaje
) {}
