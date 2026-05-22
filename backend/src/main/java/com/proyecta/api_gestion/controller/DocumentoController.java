package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IDocumentoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoListadoResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResultDTO;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/proyectos")
@PreAuthorize("hasRole('app_access')")
public class DocumentoController implements IDocumentoController {

    private final IDocumentoService documentoService;

    public DocumentoController(IDocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Override
    @GetMapping("/{proyectoId}/documentos")
    public ResponseEntity<ApiResponse<DocumentoListadoResponseDTO>> listarDocumentos(@PathVariable String proyectoId) {
        DocumentoListadoResponseDTO response = documentoService.listarDocumentos(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(response, "Documentos recuperados exitosamente"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/documentos/{tipoDocumento}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<DocumentoUploadResultDTO>> cargarDocumento(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento,
            @RequestPart("archivo") MultipartFile archivo) {
        
        DocumentoUploadResultDTO response = documentoService.cargarDocumento(proyectoId, tipoDocumento, archivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Documento cargado exitosamente"));
    }

    @Override
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/descargar")
    public ResponseEntity<Resource> descargarDocumento(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento) {
        
        Resource resource = documentoService.descargarDocumento(proyectoId, tipoDocumento);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Override
    @DeleteMapping("/{proyectoId}/documentos/{tipoDocumento}")
    public ResponseEntity<ApiResponse<Void>> eliminarDocumento(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento) {
        
        documentoService.eliminarDocumento(proyectoId, tipoDocumento);
        
        return ResponseEntity.ok(ApiResponse.success("Documento eliminado exitosamente"));
    }

    // Endpoints alternativos para tipos de documento dinámicos (ej: evidencias)
    @PostMapping(value = "/{proyectoId}/documentos/dynamic/{tipoDocumento}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<DocumentoUploadResultDTO>> cargarDocumentoDinamico(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento,
            @RequestPart("archivo") MultipartFile archivo) {
        DocumentoUploadResultDTO response = documentoService.cargarDocumento(proyectoId, tipoDocumento, archivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Documento cargado exitosamente"));
    }

    @GetMapping(value = "/{proyectoId}/documentos/dynamic/{tipoDocumento}/descargar")
    public ResponseEntity<Resource> descargarDocumentoDinamico(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento) {
        Resource resource = documentoService.descargarDocumento(proyectoId, tipoDocumento);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{proyectoId}/documentos/dynamic/{tipoDocumento}")
    public ResponseEntity<ApiResponse<Void>> eliminarDocumentoDinamico(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento) {
        documentoService.eliminarDocumento(proyectoId, tipoDocumento);
        return ResponseEntity.ok(ApiResponse.success("Documento eliminado exitosamente"));
    }
}
