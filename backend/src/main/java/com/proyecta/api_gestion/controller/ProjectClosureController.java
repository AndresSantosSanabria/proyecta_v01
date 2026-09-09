package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectClosureController;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import com.proyecta.api_gestion.service.interfaces.IStorageProvider;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProjectClosureController implements IProjectClosureController {

    private final ProjectClosureService closureService;
    private final IStorageProvider storageProvider;

    public ProjectClosureController(ProjectClosureService closureService, IStorageProvider storageProvider) {
        this.closureService = closureService;
        this.storageProvider = storageProvider;
    }

    @Override
    @PostMapping("/{id}/cierre")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:CERRAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> cerrarProyecto(
            @PathVariable String id,
            @RequestBody(required = false) CierreProyectoRequest request) {

        CierreProyectoResponse response = closureService.cerrarProyecto(id, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/cierre/solicitar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:SOLICITAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> solicitarCierre(
            @PathVariable String id,
            @RequestBody(required = false) CierreProyectoRequest request,
            Authentication authentication) {

        CierreProyectoResponse response = closureService.solicitarCierre(id, request, authentication);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/cierre/aprobar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:APROBAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> aprobarCierre(
            @PathVariable String id,
            Authentication authentication) {

        CierreProyectoResponse response = closureService.aprobarCierre(id, authentication);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/cierre/rechazar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:APROBAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> rechazarCierre(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {

        String observaciones = body.getOrDefault("observaciones", "");
        CierreProyectoResponse response = closureService.rechazarCierre(id, observaciones, authentication);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{id}/cierre/descargar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<Resource> descargarActaCierre(@PathVariable String id) {
        Resource resource = closureService.descargarActaCierre(id);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "acta-cierre.docx";
        String contentType;
        if (fileName.toLowerCase().endsWith(".pdf")) {
            contentType = "application/pdf";
        } else {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @Override
    @PostMapping(value = "/{id}/cierre/evidencia-transferencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:SOLICITAR', #id, authentication)")
    public ResponseEntity<Map<String, String>> subirEvidenciaTransferencia(
            @PathVariable String id,
            @RequestPart("evidencia") MultipartFile evidencia) {

        if (evidencia == null || evidencia.isEmpty()) {
            throw new com.proyecta.api_gestion.exception.BadRequestException("Debe seleccionar un archivo de evidencia.");
        }
        String contentType = evidencia.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new com.proyecta.api_gestion.exception.BadRequestException("Solo se permiten archivos PDF como evidencia.");
        }

        String originalName = evidencia.getOriginalFilename() != null ? evidencia.getOriginalFilename() : "evidencia.pdf";
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueName = UUID.randomUUID().toString() + "_" + safeName;

        String storedName = storageProvider.storeFile(evidencia, "cierre-transferencia", uniqueName);

        return ResponseEntity.ok(Map.of(
                "fileName", originalName,
                "storedName", storedName
        ));
    }

    @Override
    @GetMapping("/{id}/cierre/evidencia-transferencia/{fileName}")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<Resource> descargarEvidenciaTransferencia(
            @PathVariable String id,
            @PathVariable String fileName) {
        Resource resource = storageProvider.loadFileAsResource("cierre-transferencia", fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @Override
    @PostMapping("/{id}/cierre/extraordinario")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:EXTRAORDINARIO', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> cierreExtraordinario(
            @PathVariable String id,
            @RequestBody(required = false) CierreProyectoRequest request,
            Authentication authentication) {

        CierreProyectoResponse response = closureService.cierreExtraordinario(id, request, authentication);
        return ResponseEntity.ok(response);
    }
}
