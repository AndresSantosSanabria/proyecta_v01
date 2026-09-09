package com.proyecta.api_gestion.dto.audit;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemAuditLogDTO(
        UUID id,
        String usuarioId,
        String usuarioNombre,
        String usuarioRol,
        String accion,
        String modulo,
        String metodoHttp,
        String recurso,
        Integer codigoEstado,
        String estado,
        String detalle,
        String trazaError,
        String ipOrigen,
        String userAgent,
        String requestBody,
        String entidadTipo,
        String entidadId,
        String respuestaBody,
        Long duracionMs,
        LocalDateTime fechaCreacion
) {
}