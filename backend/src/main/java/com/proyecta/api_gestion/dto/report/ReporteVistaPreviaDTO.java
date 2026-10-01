package com.proyecta.api_gestion.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(example = """
    {"proyectoId":"PROY-CUN-2026-008","nombre":"Modernización de Redes LAN","avanceTotal":55.00,"directorNombre":"María Gómez","dependencia":"Secretaría de Transformación Digital","patrocinadorNombre":"Carlos Ramírez","entregablesVencidos":[{"id":101,"nombre":"Documento de alcance aprobado","fechaEntrega":"2026-07-10","faseNombre":"Planificación"}]}
    """)
public record ReporteVistaPreviaDTO(
    String proyectoId,
    String nombre,
    BigDecimal avanceTotal,
    String directorNombre,
    String dependencia,
    String patrocinadorNombre,
    List<EntregablePendienteDTO> entregablesVencidos
) {}
