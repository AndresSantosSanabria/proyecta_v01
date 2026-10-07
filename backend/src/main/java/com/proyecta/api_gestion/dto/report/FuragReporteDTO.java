package com.proyecta.api_gestion.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","nombre":"Modernización de Redes LAN","esPeti":true,"estrategiaPeti":"TECNOLOGIAS_INFORMACION","vigenciaPeti":"2026","objetivoGeneral":"Modernizar la infraestructura de red de las sedes institucionales"}
    """)
public record FuragReporteDTO(
    String proyectoId,
    String nombre,
    Boolean esPeti,
    String estrategiaPeti,
    String vigenciaPeti,
    String objetivoGeneral
) {}
