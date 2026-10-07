package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IProyectoBeneficioImpactoController;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoRequest;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoResponseDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.service.interfaces.ProyectoBeneficioImpactoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Beneficios e Impacto", description = "Endpoints de beneficios e impacto de los proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProyectoBeneficioImpactoController implements IProyectoBeneficioImpactoController {

    private final ProyectoBeneficioImpactoService service;

    public ProyectoBeneficioImpactoController(ProyectoBeneficioImpactoService service) {
        this.service = service;
    }

    @Operation(summary = "Obtener la información de beneficio e impacto de un proyecto")
    @Override
    @GetMapping("/{proyectoId}/beneficio-impacto")
    @PreAuthorize("@proyectoSecurity.canViewBenefitImpact(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> obtener(
            @Parameter(description = "Identificador del proyecto") @PathVariable String proyectoId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.obtener(proyectoId, authentication), "Informacion de beneficio e impacto obtenida"));
    }

    @Operation(
        summary = "Guardar la información de beneficio e impacto de un proyecto",
        description = "Crea o actualiza la información de beneficio e impacto, validando las reglas de negocio del formulario."
    )
    @Override
    @PutMapping("/{proyectoId}/beneficio-impacto")
    @PreAuthorize("@proyectoSecurity.canEditBenefitImpact(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> guardar(
            @Parameter(description = "Identificador del proyecto") @PathVariable String proyectoId,
            @Valid @RequestBody ProyectoBeneficioImpactoRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.guardar(proyectoId, request, authentication), "Informacion de beneficio e impacto guardada exitosamente"));
    }

    @Operation(
        summary = "Revisar la información de beneficio e impacto",
        description = "Registra la aprobación o el rechazo con observaciones de la revisión de la información, y notifica al responsable."
    )
    @PostMapping("/{proyectoId}/beneficio-impacto/revision")
    @PreAuthorize("@proyectoSecurity.canReviewBenefitImpact(#proyectoId, authentication)")
    public ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> revisar(
            @Parameter(description = "Identificador del proyecto") @PathVariable String proyectoId,
            @Parameter(description = "Indica si la información fue aprobada (true) u observada (false)") @RequestParam boolean aprobado,
            @Parameter(description = "Observaciones de la revisión (obligatorias si no se aprueba)") @RequestParam(required = false) String observaciones,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                service.revisar(proyectoId, aprobado, observaciones, authentication),
                aprobado ? "Informacion aprobada exitosamente" : "Informacion marcada como observada"));
    }
}
