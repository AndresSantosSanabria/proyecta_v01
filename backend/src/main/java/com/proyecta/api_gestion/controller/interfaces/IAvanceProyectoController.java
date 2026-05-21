package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.ProyectoAvanceSwaggerConstants;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.avance.EntregableConformidadResponseDTO;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(
    name        = ProyectoAvanceSwaggerConstants.TAG_NAME,
    description = ProyectoAvanceSwaggerConstants.TAG_DESCRIPTION
)
public interface IAvanceProyectoController {

    @Operation(
        summary     = ProyectoAvanceSwaggerConstants.SUMMARY_GET_AVANCE,
        description = ProyectoAvanceSwaggerConstants.DESCRIPTION_GET_AVANCE
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = ProyectoAvanceSwaggerConstants.RESPONSE_200_DESC,
            content = @Content(schema = @Schema(implementation = ProyectoAvanceResponseDTO.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<ProyectoAvanceResponseDTO>> getAvanceProyecto(
            @Parameter(description = "Identificador del proyecto", example = "IS-PROY-CUN-001")
            @PathVariable String proyectoId);

    @Operation(
        summary     = "Marcar entregable como COMPLETADO + subir PDF",
        description = "Marca un entregable como COMPLETADO, guarda el PDF de evidencia y lo vuelve inmutable."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Entregable completado exitosamente",
            content = @Content(schema = @Schema(implementation = EntregableConformidadResponseDTO.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> marcarCompletado(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @Parameter(description = "fecha real de entrega (yyyy-MM-dd)") @RequestParam LocalDate fechaEntrega,
            @Parameter(description = "archivo PDF de evidencia") @RequestPart("evidencia") MultipartFile evidencia);

    @Operation(
        summary     = ProyectoAvanceSwaggerConstants.SUMMARY_PATCH_AVANCE,
        description = ProyectoAvanceSwaggerConstants.DESCRIPTION_PATCH_AVANCE
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Entregable marcado como conforme",
            content = @Content(schema = @Schema(implementation = EntregableConformidadResponseDTO.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> marcarConformidad(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @Parameter(description = "true para marcar a conformidad") @RequestParam Boolean conformidad,
            @Parameter(description = "fecha real de entrega (yyyy-MM-dd)") @RequestParam LocalDate fechaEntrega,
            @Parameter(description = "archivo PDF de evidencia") @RequestPart MultipartFile evidencia);
}
