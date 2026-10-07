package com.proyecta.api_gestion.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","nombreProyecto":"Modernización de Redes LAN","directorProyecto":"María Gómez","dependencia":"Secretaría de Transformación Digital","diligencioTratamiento":true}
    """)
public record RiesgoVerificacionReporteDTO(
        String proyectoId,
        String nombreProyecto,
        String directorProyecto,
        String dependencia,
        boolean diligencioTratamiento
) {
}
