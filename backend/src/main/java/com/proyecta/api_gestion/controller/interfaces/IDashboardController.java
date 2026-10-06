package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.DashboardSwaggerConstants;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * Interfaz para el controlador de Dashboard.
 * Centraliza la documentación Swagger de todos los endpoints.
 */
@Tag(name = DashboardSwaggerConstants.TAG_NAME, description = DashboardSwaggerConstants.TAG_DESCRIPTION)
public interface IDashboardController {

    @Operation(
        summary = DashboardSwaggerConstants.SUMMARY_GET_SUMMARY,
        description = DashboardSwaggerConstants.DESCRIPTION_GET_SUMMARY
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = DashboardSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "200 OK",
                    summary = "Resumen ejecutivo obtenido exitosamente",
                    value = """
                            {
                              "success": true,
                              "message": "Resumen ejecutivo obtenido con éxito",
                              "timestamp": "2026-04-29T10:00:00",
                              "data": {
                                "totalProyectosActivos": 25,
                                "avancePromedio": 67.5,
                                "totalEntregablesConforme": 150,
                                "totalEntregablesAtrasados": 12,
                                "proyectosConAtrasos": 8,
                                "riesgoPromedio": 2.3
                              }
                            }"""
                )
            )
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary();

    @Operation(
        summary = DashboardSwaggerConstants.SUMMARY_GET_PROJECTS,
        description = DashboardSwaggerConstants.DESCRIPTION_GET_PROJECTS
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = DashboardSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(
                    name = "200 OK",
                    summary = "Lista de proyectos obtenida exitosamente",
                    value = """
                            {
                              "success": true,
                              "message": "Lista de proyectos obtenida con éxito",
                              "timestamp": "2026-04-29T10:00:00",
                              "data": [
                                {
                                  "proyectoId": "PROY-001",
                                  "nombre": "Sistema de Gestión",
                                  "avance": 75.5,
                                  "estado": "activo",
                                  "entregablesConforme": 15,
                                  "entregablesAtrasados": 2
                                }
                              ]
                            }"""
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "No hay proyectos para mostrar",
            content = @Content
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<List<DashboardProjectSummaryDTO>>> getProjectSummary();
}
