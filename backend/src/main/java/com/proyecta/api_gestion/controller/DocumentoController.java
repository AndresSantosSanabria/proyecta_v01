package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IDocumentoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoListadoResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResultDTO;
import com.proyecta.api_gestion.dto.document.DocumentoVersionHistorialResponseDTO;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import com.proyecta.api_gestion.model.DocumentoDinamico;
import com.proyecta.api_gestion.repository.DocumentoDinamicoRepository;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class DocumentoController implements IDocumentoController {

    private final IDocumentoService documentoService;
    private final DocumentoDinamicoRepository documentoDinamicoRepository;
    private final IStorageProvider storageProvider;

    public DocumentoController(IDocumentoService documentoService,
                               DocumentoDinamicoRepository documentoDinamicoRepository,
                               IStorageProvider storageProvider) {
        this.documentoService = documentoService;
        this.documentoDinamicoRepository = documentoDinamicoRepository;
        this.storageProvider = storageProvider;
    }

    @Override
    @GetMapping("/{proyectoId}/documentos")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<DocumentoListadoResponseDTO>> listarDocumentos(@PathVariable String proyectoId) {
        DocumentoListadoResponseDTO response = documentoService.listarDocumentos(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(response, "Documentos recuperados exitosamente"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/documentos/{tipoDocumento}", consumes = {"multipart/form-data"})
    @PreAuthorize("@proyectoSecurity.canAccessOperational('DOCUMENTO:CARGAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<DocumentoUploadResultDTO>> cargarDocumento(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento,
            @RequestPart("archivo") MultipartFile archivo,
            @RequestPart(value = "observacion", required = false) String observacion,
            Authentication authentication) {

        DocumentoUploadResultDTO response = documentoService.cargarDocumento(proyectoId, tipoDocumento, archivo, observacion, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Documento cargado exitosamente"));
    }

    @Override
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarDocumento(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento) {

        Resource resource = documentoService.descargarDocumento(proyectoId, tipoDocumento);
        String filename = resource.getFilename();
        boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");

        return ResponseEntity.ok()
                .contentType(isPdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (isPdf ? "inline" : "attachment") + "; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Override
    @GetMapping("/{proyectoId}/documentos/{tipoDocumento}/versiones")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<DocumentoVersionHistorialResponseDTO>> listarVersiones(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento) {
        DocumentoVersionHistorialResponseDTO response = documentoService.listarVersiones(proyectoId, tipoDocumento);
        return ResponseEntity.ok(ApiResponse.success(response, "Historial de versiones obtenido exitosamente"));
    }

    @Override
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/versiones/{numeroVersion}/archivo")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarVersion(
            @PathVariable String proyectoId,
            @PathVariable String tipoDocumento,
            @PathVariable Integer numeroVersion) {

        Resource resource = documentoService.descargarVersion(proyectoId, tipoDocumento, numeroVersion);
        String filename = resource.getFilename();
        boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");

        return ResponseEntity.ok()
                .contentType(isPdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (isPdf ? "inline" : "attachment") + "; filename=\"" + filename + "\"")
                .body(resource);
    }

    @Override
    @GetMapping(value = "/{proyectoId}/documentos-dinamicos/{documentoId}/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<Resource> descargarDocumentoDinamico(
            @PathVariable String proyectoId,
            @PathVariable Long documentoId) {

        DocumentoDinamico doc = documentoDinamicoRepository.findById(documentoId)
                .filter(d -> proyectoId.equals(d.getProyectoId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento dinamico no encontrado: " + documentoId + " para proyecto " + proyectoId));

        Resource resource = storageProvider.loadFileAsResource(doc.getRutaAlmacenamiento(), doc.getNombreAlmacenado());
        String filename = doc.getNombreOriginal();
        boolean isPdf = filename != null && filename.toLowerCase().endsWith(".pdf");

        return ResponseEntity.ok()
                .contentType(isPdf ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (isPdf ? "inline" : "attachment") + "; filename=\"" + filename + "\"")
                .body(resource);
    }
}
