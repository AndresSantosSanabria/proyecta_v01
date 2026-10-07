package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IDashboardController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.dashboard.DashboardSummaryDTO;
import com.proyecta.api_gestion.service.interfaces.DashboardService;
import io.swagger.v3.oas.annotations.media.Content;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class DashboardController implements IDashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    @GetMapping("/kpis")
    @PreAuthorize("@proyectoSecurity.canViewDashboard(authentication)")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getSummary() {
        DashboardSummaryDTO summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "KPIs globales obtenidos con éxito"));
    }

    @Override
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "204",
        description = "No hay proyectos para mostrar",
        content = @Content
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @GetMapping("/avance-por-proyecto")
    @PreAuthorize("@proyectoSecurity.canViewDashboard(authentication)")
    public ResponseEntity<ApiResponse<List<DashboardProjectSummaryDTO>>> getProjectSummary() {
        List<DashboardProjectSummaryDTO> projects = dashboardService.getProjectSummary();

        if (projects.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(ApiResponse.success(projects, "Lista de avance por proyecto obtenida con éxito"));
    }
}
