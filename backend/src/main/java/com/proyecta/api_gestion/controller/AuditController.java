package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.audit.AuditLogPageResponse;
import com.proyecta.api_gestion.dto.audit.AuditLogStatsDTO;
import com.proyecta.api_gestion.dto.audit.SystemAuditLogDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.audit.SystemAuditLogService;
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
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class AuditController {

    private final SystemAuditLogService auditLogService;

    public AuditController(SystemAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<AuditLogPageResponse>> listarLogs(
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String usuarioId,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String metodoHttp,
            @RequestParam(required = false) Integer codigoEstado,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        AuditLogPageResponse result = auditLogService.listarConFiltros(
                accion, estado, usuarioId, modulo, metodoHttp,
                codigoEstado, search, desde, hasta, page, size);

        return ResponseEntity.ok(ApiResponse.success(result, "Logs de auditoria listados correctamente"));
    }

    @GetMapping("/logs/{id}")
    public ResponseEntity<ApiResponse<SystemAuditLogDTO>> obtenerDetalle(@PathVariable UUID id) {
        SystemAuditLogDTO log = auditLogService.obtenerDetalle(id);
        return ResponseEntity.ok(ApiResponse.success(log, "Detalle del registro de auditoria"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AuditLogStatsDTO>> obtenerEstadisticas() {
        AuditLogStatsDTO stats = auditLogService.obtenerEstadisticas();
        return ResponseEntity.ok(ApiResponse.success(stats, "Estadisticas de auditoria"));
    }

    @PatchMapping("/logs/{id}/eliminar")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable UUID id) {
        auditLogService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Registro de auditoria eliminado correctamente"));
    }

    @PatchMapping("/logs/{id}/restaurar")
    public ResponseEntity<ApiResponse<Void>> restaurar(@PathVariable UUID id) {
        auditLogService.restaurar(id);
        return ResponseEntity.ok(ApiResponse.success("Registro de auditoria restaurado correctamente"));
    }
}