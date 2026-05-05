package com.proyecta.api_gestion.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record ReporteVistaPreviaDTO(
    String proyectoId,
    String nombre,
    BigDecimal avanceTotal,
    String directorNombre,
    String dependencia,
    String patrocinadorNombre,
    List<EntregablePendienteDTO> entregablesVencidos
) {}
