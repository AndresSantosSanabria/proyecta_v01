package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.project.ProjectHierarchyDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> agregarEntregable(@PathVariable String id, @PathVariable Integer hitoId, @RequestBody com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);

    @Operation(summary = "EP-ENTR-03 · Editar entregable")
    @PutMapping("/{id}/entregables/{entregableId}")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Entregable>> editarEntregable(@PathVariable String id, @PathVariable Integer entregableId, @RequestBody com.proyecta.api_gestion.dto.proyecto.EntregableDTO dto);

    @Operation(summary = "EP-ENTR-05 · Eliminar entregable")
    @DeleteMapping("/{id}/entregables/{entregableId}")
    ResponseEntity<Void> eliminarEntregable(@PathVariable String id, @PathVariable Integer entregableId);
}
