package com.proyecta.api_gestion.dto.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(example = """
    {"id":"1a2b3c4d-5e6f-4a7b-8c9d-0abcdef01234","usuarioId":"usr-7c2f9a11-4f3e-4a2b-9d5b-111111111111","usuarioNombre":"María Gómez","usuarioRol":"DIRECTOR","accion":"CREAR_PROYECTO","modulo":"PROYECTO","metodoHttp":"POST","recurso":"/api/v1/proyectos","codigoEstado":201,"estado":"EXITOSO","detalle":"Proyecto PROY-CUN-2026-008 creado","trazaError":null,"ipOrigen":"10.20.30.45","userAgent":"Mozilla/5.0","requestBody":"{"nombre":"Modernización de Redes LAN"}","entidadTipo":"Proyecto","entidadId":"PROY-CUN-2026-008","respuestaBody":"{"success":true}","duracionMs":145,"fechaCreacion":"2026-07-15T10:30:00"}
    """)
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