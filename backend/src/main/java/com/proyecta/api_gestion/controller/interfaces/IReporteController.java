package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.report.*;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Reportes", description = "Endpoints para la visualización de reportes y configuración")
public interface IReporteController {

    @Operation(summary = "Obtener configuración de reportes", description = "Lista los reportes disponibles para el menú lateral.")
    @StandardApiResponses
    @GetMapping("/configuracion")
    ResponseEntity<ApiResponse<List<ReporteConfigDTO>>> getConfiguracion();

    @Operation(summary = "Obtener vista previa de reporte", description = "Calcula métricas clave para la visualización previa de un reporte de proyecto.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Vista previa generada"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proyecto no encontrado", content = @Content)
    })
    @StandardApiResponses
    @GetMapping("/vista-previa/{proyectoId}")
    ResponseEntity<ApiResponse<ReporteVistaPreviaDTO>> getVistaPrevia(
            @Parameter(description = "ID del proyecto", example = "IS-PROY-001") @PathVariable String proyectoId);

    @Operation(summary = "Obtener estado de todos los proyectos", description = "Lista resumida de todos los proyectos con su avance.")
    @StandardApiResponses
    @GetMapping("/todos-los-proyectos")
    ResponseEntity<ApiResponse<List<ProyectoReporteResumenDTO>>> getTodosLosProyectos();

    @Operation(summary = "Obtener proyectos con retrasos", description = "Lista de proyectos que tienen entregables atrasados.")
    @StandardApiResponses
    @GetMapping("/proyectos-con-retrasos")
    ResponseEntity<ApiResponse<List<ProyectoReporteResumenDTO>>> getProyectosConRetrasos();

    @Operation(summary = "Obtener reporte de plan de comunicaciones", description = "Detalles del plan de comunicaciones de un proyecto.")
    @StandardApiResponses
    @GetMapping("/plan-comunicaciones/{proyectoId}")
    ResponseEntity<ApiResponse<PlanComunicacionesDTO>> getPlanComunicaciones(@PathVariable String proyectoId);

    @Operation(summary = "Obtener reporte FURAG", description = "Preguntas y respuestas FURAG asociadas al proyecto.")
    @StandardApiResponses
    @GetMapping("/furag/{proyectoId}")
    ResponseEntity<ApiResponse<FuragReporteDTO>> getFurag(@PathVariable String proyectoId);

    @Operation(summary = "Obtener reporte de riesgos", description = "Listado de riesgos y su estado de tratamiento.")
    @StandardApiResponses
    @GetMapping("/riesgos/{proyectoId}")
    ResponseEntity<ApiResponse<List<RiesgoReporteDTO>>> getRiesgos(@PathVariable String proyectoId);

    @Operation(summary = "Descargar reporte PDF de proyecto", description = "Genera y descarga un PDF con el estado del proyecto.")
    @GetMapping("/proyecto/{id}/descargar")
    ResponseEntity<byte[]> descargarReporteProyectoPdf(@PathVariable String id);

    @Operation(summary = "Descargar reporte PDF de portafolio", description = "Genera y descarga un PDF con el estado de todo el portafolio.")
    @GetMapping("/portafolio/descargar")
    ResponseEntity<byte[]> descargarReportePortafolioPdf();

    @Operation(summary = "Exportar portafolio a Excel", description = "Genera y descarga un Excel con la analítica del portafolio.")
    @GetMapping("/portafolio/excel")
    ResponseEntity<byte[]> descargarReportePortafolioExcel();
}
