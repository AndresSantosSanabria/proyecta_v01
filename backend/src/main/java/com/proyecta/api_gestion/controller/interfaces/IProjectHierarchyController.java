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
import org.springframework.web.bind.annotation.PathVariable;

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
}
