package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoListadoResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResultDTO;
import com.proyecta.api_gestion.dto.document.DocumentoVersionHistorialResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Modulo 6 - Documentos", description = "Endpoints para la gestion de documentos del proyecto con control de versiones")
public interface IDocumentoController {

    @Operation(summary = "EP-DOC-01 - Listar todos los documentos cargados al proyecto")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lista obtenida correctamente",
            content = @Content(schema = @Schema(implementation = DocumentoListadoResponseDTO.class))
        )
    })
    @StandardApiResponses
    @GetMapping("/{proyectoId}/documentos")
    ResponseEntity<ApiResponse<DocumentoListadoResponseDTO>> listarDocumentos(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId);

    @Operation(summary = "EP-DOC-02 - Cargar o reemplazar un documento del proyecto (requiere observacion al reemplazar)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Documento cargado exitosamente",
            content = @Content(schema = @Schema(implementation = DocumentoUploadResultDTO.class))
        )
    })
    @StandardApiResponses
    @PostMapping(value = "/{proyectoId}/documentos/{tipoDocumento}", consumes = {"multipart/form-data"})
    ResponseEntity<ApiResponse<DocumentoUploadResultDTO>> cargarDocumento(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Tipo de documento (VIABILIZACION, ACTA_CONSTITUCION, CRONOGRAMA, PLAN_COMUNICACIONES)") @PathVariable String tipoDocumento,
            @Parameter(description = "Archivo a subir") @RequestPart("archivo") MultipartFile archivo,
            @Parameter(description = "Observacion del cambio (obligatoria al reemplazar)") @RequestPart(value = "observacion", required = false) String observacion,
            Authentication authentication);

    @Operation(summary = "EP-DOC-03 - Descargar el documento actual del proyecto")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Archivo descargado correctamente",
            content = @Content(mediaType = "application/octet-stream")
        )
    })
    @StandardApiResponses
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/descargar")
    ResponseEntity<Resource> descargarDocumento(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Tipo de documento") @PathVariable String tipoDocumento);

    @Operation(summary = "EP-DOC-04 - Listar historial de versiones de un documento")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Historial obtenido correctamente",
            content = @Content(schema = @Schema(implementation = DocumentoVersionHistorialResponseDTO.class))
        )
    })
    @StandardApiResponses
    @GetMapping("/{proyectoId}/documentos/{tipoDocumento}/versiones")
    ResponseEntity<ApiResponse<DocumentoVersionHistorialResponseDTO>> listarVersiones(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Tipo de documento") @PathVariable String tipoDocumento);

    @Operation(summary = "EP-DOC-05 - Descargar una version especifica de un documento")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Archivo descargado correctamente",
            content = @Content(mediaType = "application/octet-stream")
        )
    })
    @StandardApiResponses
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/versiones/{numeroVersion}/archivo")
    ResponseEntity<Resource> descargarVersion(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Tipo de documento") @PathVariable String tipoDocumento,
            @Parameter(description = "Numero de version") @PathVariable Integer numeroVersion);

    @Operation(summary = "EP-DOC-06 - Descargar un documento dinamico por su ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Archivo descargado correctamente",
            content = @Content(mediaType = "application/octet-stream")
        )
    })
    @StandardApiResponses
    @GetMapping(value = "/{proyectoId}/documentos-dinamicos/{documentoId}/descargar")
    ResponseEntity<Resource> descargarDocumentoDinamico(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "ID del documento dinamico") @PathVariable Long documentoId);
}
