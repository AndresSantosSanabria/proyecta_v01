package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.DashboardDto.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.dto.DashboardDto.DashboardSummaryDTO;
import com.proyecta.api_gestion.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController implements IDashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
        }
    
    @Override
    @GetMapping("/projectSummary")
    public ResponseEntity<List<DashboardProjectSummaryDTO>> getProjectSummary() {
        return ResponseEntity.ok(dashboardService.getProjectSummary());
    }
    
}
