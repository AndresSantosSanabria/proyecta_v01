package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoInternoDTO;
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

import java.time.LocalDate;

@Tag(name = "Modulo Documentacion Interna", description = "Gestion de documentacion interna (solo gestores/admin)")
public interface IDocumentoInternoController {

    @Operation(summary = "EP-DI-01 - Listar documentacion interna con filtros opcionales y paginacion")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lista obtenida correctamente",
            content = @Content(schema = @Schema(implementation = DocumentoInternoDTO.Listado.class))
        )
    })
    @StandardApiResponses
    @GetMapping
    ResponseEntity<ApiResponse<DocumentoInternoDTO.Listado>> listar(
            @Parameter(description = "Filtro por nombre") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtro por anio") @RequestParam(required = false) Integer anio,
            @Parameter(description = "Filtro por descripcion") @RequestParam(required = false) String descripcion,
            @Parameter(description = "Pagina (0-based)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamano de pagina (1-200)") @RequestParam(required = false) Integer size);

    @Operation(summary = "EP-DI-02 - Cargar un documento interno (codigo DOC-ANIO-MES-DIA-HH:MM automatico)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Documento cargado exitosamente",
            content = @Content(schema = @Schema(implementation = DocumentoInternoDTO.class))
        )
    })
    @StandardApiResponses
    @PostMapping(consumes = {"multipart/form-data"})
    ResponseEntity<ApiResponse<DocumentoInternoDTO>> cargar(
            @RequestPart("nombre") String nombre,
            @RequestPart(value = "descripcion", required = false) String descripcion,
            @RequestPart(value = "fechaCreacion", required = false) String fechaCreacion,
            @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication);

    @Operation(summary = "EP-DI-03 - Descargar un documento interno")
    @StandardApiResponses
    @GetMapping("/{id}/descargar")
    ResponseEntity<Resource> descargar(
            @Parameter(description = "ID del documento") @PathVariable Long id);

    @Operation(summary = "EP-DI-04 - Ver documento interno (PDF inline)")
    @StandardApiResponses
    @GetMapping("/{id}/ver")
    ResponseEntity<Resource> ver(
            @Parameter(description = "ID del documento") @PathVariable Long id);
}
