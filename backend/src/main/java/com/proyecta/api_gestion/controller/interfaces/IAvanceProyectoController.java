package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.ProyectoAvanceSwaggerConstants;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiErrorResponseDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.dashboard.ProyectoAvanceDetalleDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(
    name        = ProyectoAvanceSwaggerConstants.TAG_NAME,
    description = ProyectoAvanceSwaggerConstants.TAG_DESCRIPTION
)
public interface IAvanceProyectoController {

    @Operation(
        summary     = ProyectoAvanceSwaggerConstants.SUMMARY_GET_AVANCE,
        description = ProyectoAvanceSwaggerConstants.DESCRIPTION_GET_AVANCE
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = ProyectoAvanceSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = ApiResponse.class),
                examples  = @ExampleObject(
                    name    = "Avance obtenido",
                    summary = "Proyecto con entregables mixtos",
                    value   = "{\n"
                            + "  \"success\": true,\n"
                            + "  \"message\": \"Avance del proyecto obtenido con éxito\",\n"
                            + "  \"timestamp\": \"2026-04-24T12:00:00\",\n"
                            + "  \"data\": {\n"
                            + "    \"avanceTotal\": 75,\n"
                            + "    \"entregablesConformes\": 15,\n"
                            + "    \"totalEntregables\": 20,\n"
                            + "    \"entregablesLabel\": \"15/20\",\n"
                            + "    \"atrasados\": 3,\n"
                            + "    \"proximosVencer\": 2\n"
                            + "  }\n"
                            + "}"
                )
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description  = ProyectoAvanceSwaggerConstants.RESPONSE_404_DESC,
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = ApiErrorResponseDTO.class),
                examples  = @ExampleObject(
                    name    = "Proyecto no encontrado",
                    summary = "El proyectoId no existe",
                    value   = "{\n"
                            + "  \"status\": 404,\n"
                            + "  \"error\": \"Not Found\",\n"
                            + "  \"message\": \"No se encontró avance para el proyecto: IS-PROY-999\",\n"
                            + "  \"path\": \"/api/proyectos/IS-PROY-999/avance\",\n"
                            + "  \"timestamp\": \"2026-04-24T12:00:00\"\n"
                            + "}"
                )
            )
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<ProyectoAvanceDetalleDTO>> getAvanceProyecto(
            @Parameter(
                description = "Identificador único del proyecto (ej. IS-PROY-CUN-001)",
                example     = "IS-PROY-001",
                required    = true
            )
            @PathVariable String proyectoId);
}
