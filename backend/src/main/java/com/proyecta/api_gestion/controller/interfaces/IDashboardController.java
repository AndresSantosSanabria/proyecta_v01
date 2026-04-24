package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.config.openapi.DashboardSwaggerConstants;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = DashboardSwaggerConstants.TAG_NAME, description = DashboardSwaggerConstants.TAG_DESCRIPTION)
public interface IDashboardController {

    @Operation(
        summary = DashboardSwaggerConstants.SUMMARY_GET_SUMMARY,
        description = DashboardSwaggerConstants.DESCRIPTION_GET_SUMMARY
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = DashboardSwaggerConstants.RESPONSE_200_DESC,
        content = @Content(schema = @Schema(implementation = ApiResponse.class))
    )
    @StandardApiResponses
    ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary();

    @Operation(
        summary = DashboardSwaggerConstants.SUMMARY_GET_PROJECTS,
        description = DashboardSwaggerConstants.DESCRIPTION_GET_PROJECTS
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = DashboardSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(schema = @Schema(implementation = ApiResponse.class))
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
