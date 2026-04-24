package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.DashboardDto.ApiErrorResponseDTO;
import com.proyecta.api_gestion.dto.DashboardDto.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.DashboardDto.DashboardSummaryDTO;
import com.proyecta.api_gestion.swagger.DashboardSwaggerConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import org.springframework.http.ResponseEntity;

@Tag(name = DashboardSwaggerConstants.TAG_NAME, description = DashboardSwaggerConstants.TAG_DESCRIPTION)
public interface IDashboardController {

    @Operation(
        summary = DashboardSwaggerConstants.SUMMARY_GET_SUMMARY,
        description = DashboardSwaggerConstants.DESCRIPTION_GET_SUMMARY
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = DashboardSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(schema = @Schema(implementation = DashboardSummaryDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = DashboardSwaggerConstants.RESPONSE_400_DESC,
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponseDTO.class),
                examples = @ExampleObject(value = DashboardSwaggerConstants.EXAMPLE_400)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = DashboardSwaggerConstants.RESPONSE_401_DESC,
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponseDTO.class),
                examples = @ExampleObject(value = DashboardSwaggerConstants.EXAMPLE_401)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = DashboardSwaggerConstants.RESPONSE_403_DESC,
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponseDTO.class),
                examples = @ExampleObject(value = DashboardSwaggerConstants.EXAMPLE_403)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = DashboardSwaggerConstants.RESPONSE_404_DESC,
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponseDTO.class),
                examples = @ExampleObject(value = DashboardSwaggerConstants.EXAMPLE_404)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = DashboardSwaggerConstants.RESPONSE_500_DESC,
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponseDTO.class),
                examples = @ExampleObject(value = DashboardSwaggerConstants.EXAMPLE_500)
            )
        ),
        @ApiResponse(
            responseCode = "503",
            description = DashboardSwaggerConstants.RESPONSE_503_DESC,
            content = @Content(
                schema = @Schema(implementation = ApiErrorResponseDTO.class),
                examples = @ExampleObject(value = DashboardSwaggerConstants.EXAMPLE_503)
            )
        )
    })
    ResponseEntity<DashboardSummaryDTO> getSummary();

	ResponseEntity<List<DashboardProjectSummaryDTO>> getProjectSummary();
}
