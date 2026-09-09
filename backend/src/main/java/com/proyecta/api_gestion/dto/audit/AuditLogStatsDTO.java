package com.proyecta.api_gestion.dto.audit;

import java.util.Map;

public record AuditLogStatsDTO(
        long totalRegistros,
        long totalExitosos,
        long totalErrores,
        Map<String, Long> porAccion,
        Map<Integer, Long> porCodigoEstado
) {
}