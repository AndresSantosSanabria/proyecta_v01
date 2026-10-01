package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.analytics.AnalyticsPortfolioDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.interfaces.AnalyticsService;
import com.proyecta.api_gestion.service.report.AnalyticsPdfGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analítica", description = "Endpoints de analítica de portafolio con exportación PDF")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Obtener la analítica del portafolio de proyectos")
    @GetMapping("/portafolio")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<AnalyticsPortfolioDTO>> getPortfolioAnalytics() {
        AnalyticsPortfolioDTO dto = analyticsService.getPortfolioAnalytics();
        return ResponseEntity.ok(ApiResponse.success(dto, "Analitica del portafolio obtenida con exito"));
    }

    @Operation(summary = "Descargar la analítica del portafolio en PDF")
    @GetMapping(value = "/portafolio/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<byte[]> downloadPortfolioAnalyticsPdf() {
        AnalyticsPortfolioDTO dto = analyticsService.getPortfolioAnalytics();
        byte[] pdfBytes = AnalyticsPdfGenerator.generate(dto);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analitica-portafolio.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}