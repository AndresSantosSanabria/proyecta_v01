package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;
import com.proyecta.api_gestion.dto.proyecto.CambioFechaRequest;
import com.proyecta.api_gestion.dto.proyecto.CambioFechaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Jerarquía de Proyectos", description = "Endpoints para obtener la estructura completa de fases e hitos")
public interface IProjectHierarchyController {

    @Operation(
        summary = "Obtener jerarquía completa del proyecto",
        description = "Retorna la estructura jerárquica completa de un proyecto con sus fases, hitos y entregables. " +
                      "Cada nivel incluye ponderaciones y avance calculado."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Jerarquía obtenida exitosamente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "200 OK",
                    summary = "Jerarquía completa",
                    value = "{\"success\": true, \"message\": \"Jerarquía obtenida con éxito\", \"data\": {\"proyectoId\": \"IS-PROY-001\", \"nombreProyecto\": \"Sistema de Gestión\", \"fases\": []}}"
                )
            )
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<ProjectHierarchyDTO>> getProjectHierarchy(
            @Parameter(description = "ID del proyecto", example = "IS-PROY-001", required = true)
            @PathVariable String id);

    // Fases
    @Operation(summary = "Agregar fase", description = "Añade una nueva fase al proyecto.")
    @PostMapping("/{id}/fases")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO>> agregarFase(@PathVariable String id, @RequestBody com.proyecta.api_gestion.dto.proyecto.FaseDTO dto);

    @Operation(summary = "Editar fase")
    @PutMapping("/{id}/fases/{faseId}")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.FaseResponseDTO>> editarFase(@PathVariable String id, @PathVariable Integer faseId, @RequestBody com.proyecta.api_gestion.dto.proyecto.FaseDTO dto);

    @Operation(summary = "Eliminar fase")
    @DeleteMapping("/{id}/fases/{faseId}")
    ResponseEntity<Void> eliminarFase(@PathVariable String id, @PathVariable Integer faseId);

    // Hitos
    @Operation(summary = "Agregar hito")
    @PostMapping("/{id}/fases/{faseId}/hitos")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO>> agregarHito(@PathVariable String id, @PathVariable Integer faseId, @RequestBody com.proyecta.api_gestion.dto.proyecto.HitoDTO dto);

    @Operation(summary = "Editar hito")
    @PutMapping("/{id}/fases/{faseId}/hitos/{hitoId}")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.dto.proyecto.HitoResponseDTO>> editarHito(@PathVariable String id, @PathVariable Integer faseId, @PathVariable Integer hitoId, @RequestBody com.proyecta.api_gestion.dto.proyecto.HitoDTO dto);

    @Operation(summary = "Eliminar hito")
    @DeleteMapping("/{id}/fases/{faseId}/hitos/{hitoId}")
    ResponseEntity<Void> eliminarHito(@PathVariable String id, @PathVariable Integer faseId, @PathVariable Integer hitoId);

    // Entregables
    @Operation(summary = "EP-ENTR-01 · Listar entregables de un proyecto", description = "Lista todos los entregables asociados a un proyecto.")
    @GetMapping("/{id}/entregables")
    ResponseEntity<ApiResponse<java.util.List<com.proyecta.api_gestion.dto.project.EntregableHierarchyDTO>>> listarEntregablesProyecto(@PathVariable String id);

    @Operation(summary = "EP-ENTR-02 · Agregar entregable")
    @PostMapping("/{id}/fases/{faseId}/hitos/{hitoId}/entregables")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> agregarEntregable(@PathVariable String id, @PathVariable Integer faseId, @PathVariable Integer hitoId, @RequestBody com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);

    @Operation(summary = "EP-ENTR-03 · Editar entregable")
    @PutMapping("/{id}/entregables/{entregableId}")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> editarEntregable(@PathVariable String id, @PathVariable Integer entregableId, @RequestBody com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);

    @Operation(summary = "EP-ENTR-05 · Eliminar entregable")
    @DeleteMapping("/{id}/entregables/{entregableId}")
    ResponseEntity<Void> eliminarEntregable(@PathVariable String id, @PathVariable Integer entregableId);

    // Cambio de fecha limite
    @Operation(summary = "EP-ENTR-06 · Cambiar fecha limite con justificacion y PDF de soporte",
               description = "Permite a Gestor/Admin modificar la fecha limite de un entregable. Requiere justificacion escrita y archivo PDF de soporte. El cambio queda registrado en historial de auditoria.")
    @PostMapping(value = "/{id}/entregables/{entregableId}/cambiar-fecha", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<CambioFechaResponse>> cambiarFecha(
            @PathVariable String id,
            @PathVariable Integer entregableId,
            @RequestPart("request") @org.springframework.lang.NonNull CambioFechaRequest request,
            @RequestPart("evidencia") MultipartFile evidencia,
            Authentication authentication);

    @Operation(summary = "EP-ENTR-07 · Obtener historial de cambios de fecha",
               description = "Retorna el historial completo de cambios de fecha limite de un entregable, ordenado por fecha descendente.")
    @GetMapping("/{id}/entregables/{entregableId}/historial-fechas")
    ResponseEntity<ApiResponse<List<CambioFechaResponse>>> historialFechas(@PathVariable String id, @PathVariable Integer entregableId);

    @Operation(summary = "EP-ENTR-08 · Descargar PDF de soporte de un cambio de fecha")
    @GetMapping("/{id}/entregables/cambios-fecha/{cambioId}/descargar")
    ResponseEntity<org.springframework.core.io.Resource> descargarPdfCambioFecha(@PathVariable String id, @PathVariable Long cambioId);
}
