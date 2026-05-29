package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.analytics.AnalyticsPortfolioDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.interfaces.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/portafolio")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<AnalyticsPortfolioDTO>> getPortfolioAnalytics() {
        AnalyticsPortfolioDTO dto = analyticsService.getPortfolioAnalytics();
        return ResponseEntity.ok(ApiResponse.success(dto, "Analitica del portafolio obtenida con exito"));
    }
}
