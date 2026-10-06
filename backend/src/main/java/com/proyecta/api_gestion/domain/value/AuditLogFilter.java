package com.proyecta.api_gestion.domain.value;

import java.time.LocalDateTime;

/**
 * Criterios de busqueda del log de auditoria compartidos entre controller, servicio y repositorio.
 */
public record AuditLogFilter(
        String accion,
        String estado,
        String usuarioId,
        String modulo,
        String metodoHttp,
        Integer codigoEstado,
        String search,
        LocalDateTime desde,
        LocalDateTime hasta) {
}
