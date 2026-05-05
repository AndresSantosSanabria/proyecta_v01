package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Tag(name = "Módulo 5 — Matriz de Riesgos", description = "Endpoints para la identificación y mitigación de amenazas en proyectos")
public interface IRiesgoController {

    @Operation(summary = "EP-RIESG-01 · Listar todos los riesgos de un proyecto")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lista obtenida correctamente",
            content = @Content(schema = @Schema(implementation = RiesgoListResponseDTO.class))
        )
    })
    @StandardApiResponses
    @GetMapping("/{proyectoId}/riesgos")
    ResponseEntity<ApiResponse<RiesgoListResponseDTO>> listarRiesgos(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId);

    @Operation(summary = "EP-RIESG-02 · Agregar un nuevo riesgo al proyecto")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Riesgo agregado exitosamente",
            content = @Content(schema = @Schema(implementation = RiesgoCreatedResponseDTO.class))
        )
    })
    @StandardApiResponses
    @PostMapping("/{proyectoId}/riesgos")
    ResponseEntity<ApiResponse<RiesgoCreatedResponseDTO>> crearRiesgo(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Valid @RequestBody RiesgoRequestDTO requestDto);

    @Operation(summary = "EP-RIESG-03 · Editar un riesgo existente")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Riesgo actualizado correctamente",
            content = @Content(schema = @Schema(implementation = RiesgoResponseDTO.class))
        )
    })
    @StandardApiResponses
    @PutMapping("/{proyectoId}/riesgos/{riesgoId}")
    ResponseEntity<ApiResponse<RiesgoResponseDTO>> actualizarRiesgo(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId,
            @Valid @RequestBody RiesgoRequestDTO requestDto);

    @Operation(summary = "EP-RIESG-04 · Eliminar un riesgo")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Riesgo eliminado correctamente"
        )
    })
    @StandardApiResponses
    @DeleteMapping("/{proyectoId}/riesgos/{riesgoId}")
    ResponseEntity<Void> eliminarRiesgo(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId);
}
