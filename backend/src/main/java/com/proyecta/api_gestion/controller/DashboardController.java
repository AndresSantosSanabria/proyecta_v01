package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IDashboardController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import com.proyecta.api_gestion.service.interfaces.DashboardService;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasRole('app_access')")
public class DashboardController implements IDashboardController {

    private final DashboardService dashboardService;
    private final ProyectoAvanceService proyectoAvanceService;

    public DashboardController(DashboardService dashboardService, ProyectoAvanceService proyectoAvanceService) {
        this.dashboardService = dashboardService;
        this.proyectoAvanceService = proyectoAvanceService;
    }

    @Override
    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary() {
        DashboardSummaryDTO summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "KPIs globales obtenidos con éxito"));
    }

    @Override
    @GetMapping("/avance-por-proyecto")
    public ResponseEntity<ApiResponse<List<DashboardProjectSummaryDTO>>> getProjectSummary() {
        List<DashboardProjectSummaryDTO> projects = dashboardService.getProjectSummary();

        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ApiResponse.success(projects, "Lista de avance por proyecto obtenida con éxito"));
    }

    @Override
    @GetMapping("/proyectos/{id}/summary")
    public ResponseEntity<ApiResponse<ProyectoSummaryDTO>> getProjectSummaryById(@PathVariable String id) {
        ProyectoSummaryDTO summary = proyectoAvanceService.obtenerResumenProyecto(id);
        return ResponseEntity.ok(ApiResponse.success(summary, "Resumen del proyecto obtenido con éxito"));
    }

    @Override
    @GetMapping("/proyectos-por-dependencia")
    public ResponseEntity<ApiResponse<List<com.proyecta.api_gestion.dto.dashboard.ProjectsByDependenciaDTO>>> getProjectsByDependencia() {
        List<com.proyecta.api_gestion.dto.dashboard.ProjectsByDependenciaDTO> result = dashboardService.getProjectsByDependencia();
        return ResponseEntity.ok(ApiResponse.success(result, "Proyectos agrupados por dependencia obtenidos con éxito"));
    }
}
