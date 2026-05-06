package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IDocumentoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.document.DocumentoListResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResponseDTO;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import com.proyecta.api_gestion.service.interfaces.IDocumentoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
public class DocumentoController implements IDocumentoController {

    private final IDocumentoService documentoService;

    public DocumentoController(IDocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Override
    @GetMapping("/{proyectoId}/documentos")
    public ResponseEntity<ApiResponse<DocumentoListResponseDTO>> listarDocumentos(@PathVariable String proyectoId) {
        DocumentoListResponseDTO response = documentoService.listarDocumentos(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lista de documentos recuperada con éxito"));
    }

    @Override
    @PostMapping(value = "/{proyectoId}/documentos/{tipoDocumento}", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<DocumentoUploadResponseDTO>> cargarDocumento(
            @PathVariable String proyectoId,
            @PathVariable TipoDocumento tipoDocumento,
            @RequestPart("archivo") MultipartFile archivo) {
        DocumentoUploadResponseDTO response = documentoService.cargarDocumento(proyectoId, tipoDocumento, archivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Documento cargado exitosamente"));
    }

    @Override
    @GetMapping(value = "/{proyectoId}/documentos/{tipoDocumento}/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> descargarDocumento(
            @PathVariable String proyectoId,
            @PathVariable TipoDocumento tipoDocumento) {
        Resource resource = documentoService.descargarDocumento(proyectoId, tipoDocumento);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
