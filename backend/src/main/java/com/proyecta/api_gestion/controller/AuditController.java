package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.domain.value.AuditLogFilter;
import com.proyecta.api_gestion.dto.audit.AuditLogPageResponse;
import com.proyecta.api_gestion.dto.audit.AuditLogStatsDTO;
import com.proyecta.api_gestion.dto.audit.SystemAuditLogDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.audit.SystemAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/auditoria")
@Tag(name = "Administración - Auditoría", description = "Endpoints de consulta y administración del log de auditoría del sistema")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class AuditController {

    private final SystemAuditLogService auditLogService;

    public AuditController(SystemAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "Listar logs de auditoría con filtros y paginación", description = "Todos los filtros son opcionales y se combinan entre sí.")
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<AuditLogPageResponse>> listarLogs(
            @Parameter(description = "Filtra por acción ejecutada") @RequestParam(required = false) String accion,
            @Parameter(description = "Filtra por estado del resultado (SUCCESS/ERROR)") @RequestParam(required = false) String estado,
            @Parameter(description = "Filtra por identificador del usuario") @RequestParam(required = false) String usuarioId,
            @Parameter(description = "Filtra por módulo del sistema") @RequestParam(required = false) String modulo,
            @Parameter(description = "Filtra por método HTTP (GET, POST, ...)") @RequestParam(required = false) String metodoHttp,
            @Parameter(description = "Filtra por código de estado HTTP devuelto") @RequestParam(required = false) Integer codigoEstado,
            @Parameter(description = "Texto libre buscado en la descripción del evento") @RequestParam(required = false) String search,
            @Parameter(description = "Fecha y hora inicio del rango (ISO date-time)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @Parameter(description = "Fecha y hora fin del rango (ISO date-time)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página") @RequestParam(defaultValue = "20") int size) {

        AuditLogPageResponse result = auditLogService.listarConFiltros(
                new AuditLogFilter(accion, estado, usuarioId, modulo, metodoHttp,
                        codigoEstado, search, desde, hasta),
                page, size);

        return ResponseEntity.ok(ApiResponse.success(result, "Logs de auditoria listados correctamente"));
    }

    @Operation(summary = "Obtener el detalle de un registro de auditoría")
    @GetMapping("/logs/{id}")
    public ResponseEntity<ApiResponse<SystemAuditLogDTO>> obtenerDetalle(
            @Parameter(description = "Identificador del registro de auditoría") @PathVariable UUID id) {
        SystemAuditLogDTO log = auditLogService.obtenerDetalle(id);
        return ResponseEntity.ok(ApiResponse.success(log, "Detalle del registro de auditoria"));
    }

    @Operation(summary = "Obtener estadísticas del log de auditoría")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AuditLogStatsDTO>> obtenerEstadisticas() {
        AuditLogStatsDTO stats = auditLogService.obtenerEstadisticas();
        return ResponseEntity.ok(ApiResponse.success(stats, "Estadisticas de auditoria"));
    }

    @Operation(summary = "Eliminar (lógicamente) un registro de auditoría")
    @PatchMapping("/logs/{id}/eliminar")
    public ResponseEntity<ApiResponse<Void>> softDelete(
            @Parameter(description = "Identificador del registro de auditoría") @PathVariable UUID id) {
        auditLogService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Registro de auditoria eliminado correctamente"));
    }

    @Operation(summary = "Restaurar un registro de auditoría eliminado")
    @PatchMapping("/logs/{id}/restaurar")
    public ResponseEntity<ApiResponse<Void>> restaurar(
            @Parameter(description = "Identificador del registro de auditoría") @PathVariable UUID id) {
        auditLogService.restaurar(id);
        return ResponseEntity.ok(ApiResponse.success("Registro de auditoria restaurado correctamente"));
    }
}