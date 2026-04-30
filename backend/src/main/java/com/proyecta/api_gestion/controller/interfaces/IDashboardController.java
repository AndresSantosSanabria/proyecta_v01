package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.DashboardSwaggerConstants;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
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
                    value = "{\n" +
                            "  \"success\": true,\n" +
                            "  \"message\": \"Resumen ejecutivo obtenido con éxito\",\n" +
                            "  \"timestamp\": \"2026-04-29T10:00:00\",\n" +
                            "  \"data\": {\n" +
                            "    \"totalProyectosActivos\": 25,\n" +
                            "    \"avancePromedio\": 67.5,\n" +
                            "    \"totalEntregablesConforme\": 150,\n" +
                            "    \"totalEntregablesAtrasados\": 12,\n" +
                            "    \"proyectosConAtrasos\": 8,\n" +
                            "    \"riesgoPromedio\": 2.3\n" +
                            "  }\n" +
                            "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = DashboardSwaggerConstants.RESPONSE_503_DESC,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.proyecta.api_gestion.dto.common.ApiErrorResponseDTO.class),
                examples = @ExampleObject(
                    name = "503 Service Unavailable",
                    summary = "Base de datos no disponible",
                    value = "{\n" +
                            "  \"status\": 503,\n" +
                            "  \"error\": \"Service Unavailable\",\n" +
                            "  \"message\": \"Servicio de base de datos no disponible\",\n" +
                            "  \"path\": \"/api/dashboard/summary\",\n" +
                            "  \"timestamp\": \"2026-04-29T10:00:00\"\n" +
                            "}"
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
                    value = "{\n" +
                            "  \"success\": true,\n" +
                            "  \"message\": \"Lista de proyectos obtenida con éxito\",\n" +
                            "  \"timestamp\": \"2026-04-29T10:00:00\",\n" +
                            "  \"data\": [\n" +
                            "    {\n" +
                            "      \"proyectoId\": \"IS-PROY-001\",\n" +
                            "      \"nombre\": \"Sistema de Gestión\",\n" +
                            "      \"avance\": 75.5,\n" +
                            "      \"estado\": \"activo\",\n" +
                            "      \"entregablesConforme\": 15,\n" +
                            "      \"entregablesAtrasados\": 2\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "No hay proyectos para mostrar",
            content = @Content
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "503",
            description = DashboardSwaggerConstants.RESPONSE_503_DESC,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = com.proyecta.api_gestion.dto.common.ApiErrorResponseDTO.class),
                examples = @ExampleObject(
                    name = "503 Service Unavailable",
                    summary = "Base de datos no disponible",
                    value = "{\n" +
                            "  \"status\": 503,\n" +
                            "  \"error\": \"Service Unavailable\",\n" +
                            "  \"message\": \"Servicio de base de datos no disponible\",\n" +
                            "  \"path\": \"/api/dashboard/projectSummary\",\n" +
                            "  \"timestamp\": \"2026-04-29T10:00:00\"\n" +
                            "}"
                )
            )
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<List<DashboardProjectSummaryDTO>>> getProjectSummary();

    @Operation(
        summary = DashboardSwaggerConstants.SUMMARY_GET_PROJECT_BY_ID,
        description = DashboardSwaggerConstants.DESCRIPTION_GET_PROJECT_BY_ID
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = DashboardSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = DashboardSwaggerConstants.RESPONSE_404_DESC,
            content = @Content
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<ProyectoSummaryDTO>> getProjectSummaryById(@org.springframework.web.bind.annotation.PathVariable String id);
}
