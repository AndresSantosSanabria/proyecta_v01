package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoSolucionAdjuntoDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

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

    @Operation(summary = "EP-RIESG-05 · Verificar tratamiento de un riesgo", description = "Marca un riesgo como tratado y registra la verificación.")
    @StandardApiResponses
    @PatchMapping("/{proyectoId}/riesgos/{riesgoId}/tratamiento")
    ResponseEntity<ApiResponse<Void>> verificarTratamiento(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId,
            @RequestBody String verificacion);

    @Operation(summary = "EP-RIESG-06 · Listar soluciones cargadas para un riesgo")
    @StandardApiResponses
    @GetMapping("/{proyectoId}/riesgos/{riesgoId}/soluciones")
    ResponseEntity<ApiResponse<java.util.List<RiesgoSolucionAdjuntoDTO>>> listarSoluciones(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId);

    @Operation(summary = "EP-RIESG-07 · Agregar varias soluciones a un riesgo", description = "Permite cargar uno o varios PDFs como soporte de solución para el riesgo.")
    @StandardApiResponses
    @PostMapping(value = "/{proyectoId}/riesgos/{riesgoId}/soluciones", consumes = {"multipart/form-data"})
    ResponseEntity<ApiResponse<java.util.List<RiesgoSolucionAdjuntoDTO>>> agregarSoluciones(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId,
            @Parameter(description = "Archivos PDF de solución") @RequestPart("archivos") MultipartFile[] archivos);

    @Operation(summary = "EP-RIESG-08 · Descargar un PDF de solución")
    @StandardApiResponses
    @GetMapping("/{proyectoId}/riesgos/{riesgoId}/soluciones/{solucionId}/descargar")
    ResponseEntity<Resource> descargarSolucion(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del riesgo") @PathVariable Integer riesgoId,
            @Parameter(description = "ID del adjunto") @PathVariable Long solucionId);
}
