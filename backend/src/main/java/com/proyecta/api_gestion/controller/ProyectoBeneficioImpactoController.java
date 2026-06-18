package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProyectoBeneficioImpactoController;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoRequest;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoResponseDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProyectoBeneficioImpactoController implements IProyectoBeneficioImpactoController {

    private final ProyectoBeneficioImpactoService service;

    public ProyectoBeneficioImpactoController(ProyectoBeneficioImpactoService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/{proyectoId}/beneficio-impacto")
    @PreAuthorize("@proyectoSecurity.canViewBenefitImpact(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> obtener(@PathVariable String proyectoId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.obtener(proyectoId, authentication), "Informacion de beneficio e impacto obtenida"));
    }

    @Override
    @PutMapping("/{proyectoId}/beneficio-impacto")
    @PreAuthorize("@proyectoSecurity.canEditBenefitImpact(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> guardar(
            @PathVariable String proyectoId,
            @Valid @RequestBody ProyectoBeneficioImpactoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.guardar(proyectoId, request, authentication), "Informacion de beneficio e impacto guardada exitosamente"));
    }

    @PostMapping("/{proyectoId}/beneficio-impacto/revision")
    public ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> revisar(
            @PathVariable String proyectoId,
            @RequestParam boolean aprobado,
            @RequestParam(required = false) String observaciones,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                service.revisar(proyectoId, aprobado, observaciones, authentication),
                aprobado ? "Informacion aprobada exitosamente" : "Informacion marcada como observada"));
    }
}
