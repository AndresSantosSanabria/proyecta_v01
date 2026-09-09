package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IReporteController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ReporteController implements IReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Override
    @GetMapping("/configuracion")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<List<ReporteConfigDTO>>> getConfiguracion() {
        List<ReporteConfigDTO> config = reporteService.obtenerConfiguracionReportes();
        return ResponseEntity.ok(ApiResponse.success(config, "Configuracion de reportes obtenida con exito"));
    }

    @Override
    @GetMapping("/vista-previa/{proyectoId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ReporteVistaPreviaDTO>> getVistaPrevia(@PathVariable String proyectoId) {
        ReporteVistaPreviaDTO dto = reporteService.obtenerVistaPrevia(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Vista previa generada con exito"));
    }

    @Override
    @GetMapping("/todos-los-proyectos")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<List<ProyectoReporteResumenDTO>>> getTodosLosProyectos() {
        List<ProyectoReporteResumenDTO> reportes = reporteService.obtenerTodosLosProyectos();
        return ResponseEntity.ok(ApiResponse.success(reportes, "Reporte de todos los proyectos obtenido con exito"));
    }

    @Override
    @GetMapping("/proyectos-con-retrasos")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<List<ProyectoReporteResumenDTO>>> getProyectosConRetrasos() {
        List<ProyectoReporteResumenDTO> reportes = reporteService.obtenerProyectosConRetrasos();
        return ResponseEntity.ok(ApiResponse.success(reportes, "Reporte de proyectos con retrasos obtenido con exito"));
    }

    @Override
    @GetMapping("/furag/{proyectoId}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<FuragReporteDTO>> getFurag(@PathVariable String proyectoId) {
        FuragReporteDTO dto = reporteService.obtenerFurag(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Reporte FURAG obtenido con exito"));
    }

    @Override
    @GetMapping("/riesgos")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<List<RiesgoVerificacionReporteDTO>>> getRiesgos() {
        List<RiesgoVerificacionReporteDTO> riesgos = reporteService.obtenerVerificacionRiesgos();
        return ResponseEntity.ok(ApiResponse.success(riesgos, "Reporte de verificacion de tratamiento a riesgos obtenido con exito"));
    }

    @Override
    @GetMapping("/proyecto/{id}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<byte[]> descargarReporteProyectoPdf(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "resumido") String detailMode) {
        byte[] content = reporteService.generarReporteProyectoPdf(id, detailMode);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-estado-proyecto-especifico-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/portafolio/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<byte[]> descargarReportePortafolioPdf(
            @RequestParam(required = false, defaultValue = "resumido") String detailMode) {
        byte[] content = reporteService.generarReportePortafolioPdf(detailMode);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-estado-todos-los-proyectos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/proyectos-con-retrasos/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<byte[]> descargarReporteProyectosConRetrasosPdf(
            @RequestParam(required = false, defaultValue = "resumido") String detailMode) {
        byte[] content = reporteService.generarReporteProyectosConRetrasosPdf(detailMode);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-proyectos-con-retrasos-en-la-fecha-de-entrega.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/plan-comunicaciones/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<byte[]> descargarReportePlanComunicacionesPdf(
            @RequestParam(required = false, defaultValue = "resumido") String detailMode) {
        byte[] content = reporteService.generarReportePlanComunicacionesPdf(detailMode);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-plan-comunicaciones.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/furag/{proyectoId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<byte[]> descargarReporteFuragPdf(
            @PathVariable String proyectoId,
            @RequestParam(required = false, defaultValue = "resumido") String detailMode) {
        byte[] content = reporteService.generarReporteFuragPdf(proyectoId, detailMode);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-furag-" + proyectoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/riesgos/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<byte[]> descargarReporteRiesgosPdf(
            @RequestParam(required = false, defaultValue = "resumido") String detailMode) {
        byte[] content = reporteService.generarReporteRiesgosPdf(detailMode);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-verificacion-tratamiento-a-riesgos.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/portafolio/excel")
    @PreAuthorize("@proyectoSecurity.canAccessOwnProjects(authentication)")
    public ResponseEntity<byte[]> descargarReportePortafolioExcel(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String dependency,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String peti,
            Authentication authentication) {
        byte[] content = reporteService.generarReportePortafolioExcel(authentication, query, dependency, status, peti);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=Consolidado Seguimiento Proyectos PETI.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }

    @Override
    @GetMapping("/proyecto/{proyectoId}/excel")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('REPORTE:DESCARGAR_ACTUAL', #proyectoId, authentication)")
    public ResponseEntity<byte[]> descargarReporteActualProyectoExcel(@PathVariable String proyectoId) {
        byte[] content = reporteService.generarReporteActualProyectoExcel(proyectoId);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-actual-proyecto-" + proyectoId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
