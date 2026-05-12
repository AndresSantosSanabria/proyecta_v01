package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IReporteController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.service.interfaces.ReporteService;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
@CrossOrigin(origins = "*")
public class ReporteController implements IReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @Override
    @GetMapping("/configuracion")
    public ResponseEntity<ApiResponse<List<ReporteConfigDTO>>> getConfiguracion() {
        List<ReporteConfigDTO> config = reporteService.obtenerConfiguracionReportes();
        return ResponseEntity.ok(ApiResponse.success(config, "Configuración de reportes obtenida con éxito"));
    }

    @Override
    @GetMapping("/vista-previa/{proyectoId}")
    public ResponseEntity<ApiResponse<ReporteVistaPreviaDTO>> getVistaPrevia(@PathVariable String proyectoId) {
        ReporteVistaPreviaDTO dto = reporteService.obtenerVistaPrevia(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Vista previa generada con éxito"));
    }

    @Override
    @GetMapping("/todos-los-proyectos")
    public ResponseEntity<ApiResponse<List<ProyectoReporteResumenDTO>>> getTodosLosProyectos() {
        List<ProyectoReporteResumenDTO> reportes = reporteService.obtenerTodosLosProyectos();
        return ResponseEntity.ok(ApiResponse.success(reportes, "Reporte de todos los proyectos obtenido con éxito"));
    }

    @Override
    @GetMapping("/proyectos-con-retrasos")
    public ResponseEntity<ApiResponse<List<ProyectoReporteResumenDTO>>> getProyectosConRetrasos() {
        List<ProyectoReporteResumenDTO> reportes = reporteService.obtenerProyectosConRetrasos();
        return ResponseEntity.ok(ApiResponse.success(reportes, "Reporte de proyectos con retrasos obtenido con éxito"));
    }

    @Override
    @GetMapping("/plan-comunicaciones/{proyectoId}")
    public ResponseEntity<ApiResponse<PlanComunicacionesDTO>> getPlanComunicaciones(@PathVariable String proyectoId) {
        PlanComunicacionesDTO dto = reporteService.obtenerPlanComunicaciones(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Reporte de plan de comunicaciones obtenido con éxito"));
    }

    @Override
    @GetMapping("/furag/{proyectoId}")
    public ResponseEntity<ApiResponse<FuragReporteDTO>> getFurag(@PathVariable String proyectoId) {
        ReporteService service = reporteService;
        FuragReporteDTO dto = service.obtenerFurag(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + proyectoId));
        return ResponseEntity.ok(ApiResponse.success(dto, "Reporte FURAG obtenido con éxito"));
    }

    @Override
    @GetMapping("/riesgos/{proyectoId}")
    public ResponseEntity<ApiResponse<List<RiesgoReporteDTO>>> getRiesgos(@PathVariable String proyectoId) {
        List<RiesgoReporteDTO> riesgos = reporteService.obtenerRiesgos(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(riesgos, "Reporte de riesgos obtenido con éxito"));
    }

    @Override
    public ResponseEntity<byte[]> descargarReporteProyectoPdf(String id) {
        byte[] content = reporteService.generarReporteProyectoPdf(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte-proyecto-" + id + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    public ResponseEntity<byte[]> descargarReportePortafolioPdf() {
        byte[] content = reporteService.generarReportePortafolioPdf();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=reporte-portafolio.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(content);
    }

    @Override
    public ResponseEntity<byte[]> descargarReportePortafolioExcel() {
        byte[] content = reporteService.generarReportePortafolioExcel();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=analitica-portafolio.xlsx")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(content);
    }
}
