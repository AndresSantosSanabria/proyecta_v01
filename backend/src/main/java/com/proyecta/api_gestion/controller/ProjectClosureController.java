package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProjectClosureController;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import com.proyecta.api_gestion.service.interfaces.ProjectClosureService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:CERRAR', #id, authentication)")
    public ResponseEntity<CierreProyectoResponse> cerrarProyecto(
            @PathVariable String id,
            @Valid @RequestBody CierreProyectoRequest request) {

        CierreProyectoResponse response = closureService.cerrarProyecto(id, request);
        return ResponseEntity.ok(response);
    }
}
