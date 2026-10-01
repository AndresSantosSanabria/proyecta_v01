package com.proyecta.api_gestion.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"id":"REPORTE_SEMANAL_AVANCE","nombre":"Reporte semanal de avance","descripcion":"Consolida avance, entregables atrasados y riesgos abiertos por proyecto"}
    """)
public record ReporteConfigDTO(
    String id,
    String nombre,
    String descripcion
) {}
