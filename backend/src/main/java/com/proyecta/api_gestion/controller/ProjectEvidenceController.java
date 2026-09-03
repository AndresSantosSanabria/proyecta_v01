package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.avance.ProjectEvidenceDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.interfaces.ProjectEvidenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProjectEvidenceController {

    private final ProjectEvidenceService projectEvidenceService;

    public ProjectEvidenceController(ProjectEvidenceService projectEvidenceService) {
        this.projectEvidenceService = projectEvidenceService;
    }

    @GetMapping("/{proyectoId}/evidencias")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<List<ProjectEvidenceDTO>>> listarEvidencias(
            @PathVariable String proyectoId,
            @RequestParam(required = false) String categoria) {

        List<ProjectEvidenceDTO> evidencias = projectEvidenceService.listarEvidencias(proyectoId, categoria);
        return ResponseEntity.ok(ApiResponse.success(evidencias, "Evidencias del proyecto obtenidas con exito"));
    }
}
