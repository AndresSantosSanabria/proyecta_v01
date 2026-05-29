package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IReporteController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
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
    @GetMapping("/plan-comunicaciones/{proyectoId}")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<PlanComunicacionesDTO>> getPlanComunicaciones(@PathVariable String proyectoId) {
        PlanComunicacionesDTO dto = reporteService.obtenerPlanComunicaciones(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Reporte de plan de comunicaciones obtenido con exito"));
    }

    @Override
    @GetMapping("/furag/{proyectoId}")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<FuragReporteDTO>> getFurag(@PathVariable String proyectoId) {
        FuragReporteDTO dto = reporteService.obtenerFurag(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Reporte FURAG obtenido con exito"));
    }

    @Override
    @GetMapping("/riesgos/{proyectoId}")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<List<RiesgoReporteDTO>>> getRiesgos(@PathVariable String proyectoId) {
        List<RiesgoReporteDTO> riesgos = reporteService.obtenerRiesgos(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(riesgos, "Reporte de riesgos obtenido con exito"));
    }

    @Override
    @GetMapping("/proyecto/{id}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<byte[]> descargarReporteProyectoPdf(@PathVariable String id) {
        byte[] content = reporteService.generarReporteProyectoPdf(id);
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
    public ResponseEntity<byte[]> descargarReportePortafolioPdf() {
        byte[] content = reporteService.generarReportePortafolioPdf();
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
    public ResponseEntity<byte[]> descargarReporteProyectosConRetrasosPdf() {
        byte[] content = reporteService.generarReporteProyectosConRetrasosPdf();
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-proyectos-con-retrasos-en-la-fecha-de-entrega.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/plan-comunicaciones/{proyectoId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<byte[]> descargarReportePlanComunicacionesPdf(@PathVariable String proyectoId) {
        byte[] content = reporteService.generarReportePlanComunicacionesPdf(proyectoId);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-plan-comunicaciones-" + proyectoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/furag/{proyectoId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<byte[]> descargarReporteFuragPdf(@PathVariable String proyectoId) {
        byte[] content = reporteService.generarReporteFuragPdf(proyectoId);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-furag-" + proyectoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/riesgos/{proyectoId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<byte[]> descargarReporteRiesgosPdf(@PathVariable String proyectoId) {
        byte[] content = reporteService.generarReporteRiesgosPdf(proyectoId);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("Content-Disposition", "attachment; filename=reporte-verificacion-tratamiento-a-riesgos-" + proyectoId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    @GetMapping("/portafolio/excel")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<byte[]> descargarReportePortafolioExcel() {
        byte[] content = reporteService.generarReportePortafolioExcel();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=analitica-portafolio.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
