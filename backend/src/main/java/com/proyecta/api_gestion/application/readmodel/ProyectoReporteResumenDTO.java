package com.proyecta.api_gestion.application.readmodel;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(example = """
    {"id":"PROY-CUN-2026-008","nombre":"Modernización de Redes LAN","avance":55.00,"dependencia":"Secretaría de Transformación Digital","estado":"ACTIVO","entregablesAtrasados":2}
    """)
public record ProyectoReporteResumenDTO(
    String id,
    String nombre,
    BigDecimal avance,
    String dependencia,
    String estado,
    Long entregablesAtrasados
) {}
