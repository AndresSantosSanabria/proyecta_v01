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
import org.springframework.security.core.Authentication;
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
        summary     = "Subir evidencia de entregable",
        description = "Guarda el PDF de evidencia y deja el entregable en estado de revision pendiente de aprobacion."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Evidencia registrada exitosamente",
            content = @Content(schema = @Schema(implementation = EntregableConformidadResponseDTO.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> registrarEvidencia(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @Parameter(description = "fecha real de entrega (yyyy-MM-dd)") @RequestParam LocalDate fechaEntrega,
            @Parameter(description = "archivo PDF de evidencia") @RequestPart("evidencia") MultipartFile evidencia,
            Authentication authentication);

    @Operation(
        summary     = ProyectoAvanceSwaggerConstants.SUMMARY_PATCH_APROBAR,
        description = ProyectoAvanceSwaggerConstants.DESCRIPTION_PATCH_APROBAR
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Entregable aprobado por el gestor",
            content = @Content(schema = @Schema(implementation = EntregableConformidadResponseDTO.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> aprobarEntregable(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @Parameter(description = "Observacion opcional de aprobacion") @RequestParam(required = false) String observacion,
            Authentication authentication);

    @Operation(
        summary     = "Rechazar entregable",
        description = "Marca el entregable como rechazado para que el asignado corrija y vuelva a cargar la evidencia."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description  = "Entregable rechazado por el gestor",
            content = @Content(schema = @Schema(implementation = EntregableConformidadResponseDTO.class))
        )
    })
    @StandardApiResponses
    ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> rechazarEntregable(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @Parameter(description = "Motivo del rechazo") @RequestParam String observacion,
            Authentication authentication);
}
