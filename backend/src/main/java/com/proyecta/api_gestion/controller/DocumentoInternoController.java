package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IDocumentoInternoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoInternoDTO;
import com.proyecta.api_gestion.domain.exception.BadRequestException;
import com.proyecta.api_gestion.dto.document.DocumentoInternoDownload;
import com.proyecta.api_gestion.service.interfaces.IDocumentoInternoService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/documentos-internos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class DocumentoInternoController implements IDocumentoInternoController {

    private final IDocumentoInternoService documentoInternoService;

    public DocumentoInternoController(IDocumentoInternoService documentoInternoService) {
        this.documentoInternoService = documentoInternoService;
    }

    @Override
    @GetMapping
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('DOCUMENTO_INTERNO:VER', authentication)")
    public ResponseEntity<ApiResponse<DocumentoInternoDTO.Listado>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page != null && page < 0) {
            throw new BadRequestException("El numero de pagina no puede ser negativo.");
        }
        if (size != null && (size < 1 || size > 200)) {
            throw new BadRequestException("El tamano de pagina debe estar entre 1 y 200.");
        }
        DocumentoInternoDTO.Listado listado =
                documentoInternoService.listar(nombre, anio, descripcion, page, size);
        return ResponseEntity.ok(ApiResponse.success(listado, "Documentacion interna recuperada exitosamente"));
    }

    @Override
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Documento cargado exitosamente",
        content = @Content(schema = @Schema(implementation = DocumentoInternoDTO.class))
    )
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('DOCUMENTO_INTERNO:CARGAR', authentication)")
    public ResponseEntity<ApiResponse<DocumentoInternoDTO>> cargar(
            @Parameter(description = "Nombre del documento interno") @RequestPart("nombre") String nombre,
            @Parameter(description = "Descripcion del documento interno") @RequestPart(value = "descripcion", required = false) String descripcion,
            @Parameter(description = "Fecha de creacion en formato yyyy-MM-dd") @RequestPart(value = "fechaCreacion", required = false) String fechaCreacion,
            @Parameter(description = "Archivo a subir") @RequestPart("archivo") MultipartFile archivo,
            Authentication authentication) {

        LocalDate fecha = parseFecha(fechaCreacion);
        DocumentoInternoDTO response = documentoInternoService.cargar(nombre, descripcion, fecha, archivo, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Documento interno cargado exitosamente"));
    }

    @Override
    @GetMapping("/{id}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('DOCUMENTO_INTERNO:VER', authentication)")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        DocumentoInternoDownload download = documentoInternoService.descargar(id);
        return buildFileResponse(download, false);
    }

    @Override
    @GetMapping("/{id}/ver")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('DOCUMENTO_INTERNO:VER', authentication)")
    public ResponseEntity<Resource> ver(@PathVariable Long id) {
        DocumentoInternoDownload download = documentoInternoService.ver(id);
        return buildFileResponse(download, true);
    }

    private LocalDate parseFecha(String fechaCreacion) {
        if (fechaCreacion == null || fechaCreacion.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fechaCreacion.trim());
        } catch (DateTimeParseException _) {
            throw new BadRequestException("Fecha de creacion invalida. Use el formato YYYY-MM-DD.");
        }
    }

    private ResponseEntity<Resource> buildFileResponse(DocumentoInternoDownload download, boolean inline) {
        String filename = sanitizeContentDispositionFilename(download.filename());
        boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf")
                || (download.mimeType() != null && download.mimeType().equalsIgnoreCase("application/pdf"));
        String disposition = (inline && isPdf) ? "inline" : "attachment";
        MediaType mediaType = isPdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + filename + "\"")
                .body(download.resource());
    }

    private String sanitizeContentDispositionFilename(String filename) {
        return com.proyecta.api_gestion.infrastructure.HttpHeaderSanitizer.safeFileName(filename);
    }
}
