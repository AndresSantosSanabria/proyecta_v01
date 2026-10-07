package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.cronograma.CronogramaResponseDTO;
import com.proyecta.api_gestion.dto.cronograma.CronogramaUploadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Módulo 4 — Cronograma", description = "Endpoints para la gestión del cronograma del proyecto (Metadatos, Gantt y PDF)")
public interface ICronogramaController {

    @Operation(summary = "EP-CRON-01 · Obtener el cronograma del proyecto", description = "Obtener el cronograma del proyecto (metadata, hitos, vista Gantt y URL de descarga del PDF).")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Cronograma obtenido correctamente",
            content = @Content(schema = @Schema(implementation = CronogramaResponseDTO.class))
        )
    })
    @StandardApiResponses
    @GetMapping("/{proyectoId}/cronograma")
    ResponseEntity<ApiResponse<CronogramaResponseDTO>> obtenerCronograma(
            @Parameter(description = "ID del proyecto", required = true) @PathVariable String proyectoId);

    @Operation(summary = "EP-CRON-02 · Cargar o reemplazar el PDF del cronograma", description = "Cargar o reemplazar el PDF del cronograma (max 20MB).")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Cronograma cargado correctamente",
            content = @Content(schema = @Schema(implementation = CronogramaUploadResponseDTO.class))
        )
    })
    @StandardApiResponses
    @PostMapping(value = "/{proyectoId}/cronograma", consumes = "multipart/form-data")
    ResponseEntity<ApiResponse<CronogramaUploadResponseDTO>> cargarCronograma(
            @Parameter(description = "ID del proyecto", required = true) @PathVariable String proyectoId,
            @Parameter(description = "Archivo PDF del cronograma") @RequestPart("archivo") MultipartFile archivo);

    @Operation(summary = "EP-CRON-03 · Descargar el PDF del cronograma", description = "Descargar el PDF del cronograma del proyecto.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Archivo descargado correctamente",
            content = @Content(mediaType = "application/pdf")
        )
    })
    @StandardApiResponses
    @GetMapping(value = "/{proyectoId}/cronograma/descargar", produces = "application/pdf")
    ResponseEntity<Resource> descargarCronograma(
            @Parameter(description = "ID del proyecto", required = true) @PathVariable String proyectoId);
}
