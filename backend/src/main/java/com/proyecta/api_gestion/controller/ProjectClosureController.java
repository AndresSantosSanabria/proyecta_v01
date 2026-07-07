package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectClosureController;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProjectClosureController implements IProjectClosureController {

    private final ProjectClosureService closureService;

    public ProjectClosureController(ProjectClosureService closureService) {
        this.closureService = closureService;
    }

    @Override
    @PostMapping("/{id}/cierre")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:CERRAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> cerrarProyecto(
            @PathVariable String id,
            @Valid @RequestBody CierreProyectoRequest request) {

        CierreProyectoResponse response = closureService.cerrarProyecto(id, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/{id}/cierre/solicitar")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('CIERRE:SOLICITAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> solicitarCierre(
            @PathVariable String id,
            Authentication authentication) {

        CierreProyectoResponse response = closureService.solicitarCierre(id, authentication);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping(value = "/{id}/cierre/descargar", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<Resource> descargarActaCierre(@PathVariable String id) {
        Resource resource = closureService.descargarActaCierre(id);
        String fileName = resource.getFilename() != null ? resource.getFilename() : "acta-cierre.pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
