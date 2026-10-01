package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.avance.ProjectEvidenceDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.interfaces.ProjectEvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Evidencias de Proyecto", description = "Endpoints de carga y descarga de evidencias asociadas a riesgos y entregables")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProjectEvidenceController {

    private final ProjectEvidenceService projectEvidenceService;

    public ProjectEvidenceController(ProjectEvidenceService projectEvidenceService) {
        this.projectEvidenceService = projectEvidenceService;
    }

    @Operation(
        summary = "Listar las evidencias de un proyecto",
        description = "Retorna las evidencias del proyecto, con filtro opcional por categoría. Requiere permiso PROYECTO:VER sobre el proyecto."
    )
    @GetMapping("/{proyectoId}/evidencias")
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<List<ProjectEvidenceDTO>>> listarEvidencias(
            @Parameter(description = "Identificador del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Filtra las evidencias por categoría") @RequestParam(required = false) String categoria) {

        List<ProjectEvidenceDTO> evidencias = projectEvidenceService.listarEvidencias(proyectoId, categoria);
        return ResponseEntity.ok(ApiResponse.success(evidencias, "Evidencias del proyecto obtenidas con exito"));
    }
}
