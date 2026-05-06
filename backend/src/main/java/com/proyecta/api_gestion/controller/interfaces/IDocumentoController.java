package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoListResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResponseDTO;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
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

@Tag(name = "Módulo 6 — Documentos", description = "Endpoints para la gestión de documentos del proyecto")
public interface IDocumentoController {

    @Operation(summary = "EP-DOC-01 · Listar los documentos cargados al proyecto con sus estados")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lista obtenida correctamente",
            content = @Content(schema = @Schema(implementation = DocumentoListResponseDTO.class))
        )
    })
    @StandardApiResponses
    @GetMapping("/{proyectoId}/documentos")
    ResponseEntity<ApiResponse<DocumentoListResponseDTO>> listarDocumentos(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId);

    @Operation(summary = "EP-DOC-02 · Cargar o reemplazar un documento del proyecto")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Documento cargado exitosamente",
            content = @Content(schema = @Schema(implementation = DocumentoUploadResponseDTO.class))
        )
    })
    @StandardApiResponses
    @PostMapping(value = "/{proyectoId}/documentos/{tipoDocumento}", consumes = {"multipart/form-data"})
    ResponseEntity<ApiResponse<DocumentoUploadResponseDTO>> cargarDocumento(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Tipo de documento") @PathVariable TipoDocumento tipoDocumento,
            @Parameter(description = "Archivo PDF") @RequestPart("archivo") MultipartFile archivo);

    @Operation(summary = "EP-DOC-03 · Descargar un documento específico del proyecto")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Archivo descargado correctamente",
            content = @Content(mediaType = "application/pdf")
        )
    })
    @StandardApiResponses
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/descargar", produces = "application/pdf")
    ResponseEntity<Resource> descargarDocumento(
            @Parameter(description = "ID del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Tipo de documento") @PathVariable TipoDocumento tipoDocumento);
}
