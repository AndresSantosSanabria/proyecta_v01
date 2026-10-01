package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.config.PetiCatalogDTO;
import com.proyecta.api_gestion.service.config.PetiCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracion/catalogos")
@Tag(name = "Configuración - Catálogos", description = "Endpoints de catálogos de configuración (PETI)")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ConfiguracionCatalogoController {

    private final PetiCatalogService petiCatalogService;

    public ConfiguracionCatalogoController(PetiCatalogService petiCatalogService) {
        this.petiCatalogService = petiCatalogService;
    }

    @Operation(summary = "Obtener el catálogo PETI")
    @GetMapping("/peti")
    public ResponseEntity<ApiResponse<PetiCatalogDTO>> obtenerCatalogoPeti() {
        return ResponseEntity.ok(ApiResponse.success(
                petiCatalogService.getCatalog(),
                "Catalogo PETI obtenido correctamente"));
    }
}
