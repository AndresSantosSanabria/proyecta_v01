package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IRiesgoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.MatrizRiesgoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.service.IRiesgoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class RiesgoController implements IRiesgoController {

    private final IRiesgoService riesgoService;

    public RiesgoController(IRiesgoService riesgoService) {
        this.riesgoService = riesgoService;
    }

    @Override
    @GetMapping("/{proyectoId}/riesgos")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoListResponseDTO>> listarRiesgos(@PathVariable String proyectoId) {
        RiesgoListResponseDTO riesgos = riesgoService.getRisksByProject(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(riesgos, "Matriz de riesgos recuperada con exito"));
    }

    @GetMapping("/riesgos/matriz")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<java.util.List<MatrizRiesgoDTO>>> listarMatrizRiesgos() {
        return ResponseEntity.ok(ApiResponse.success(riesgoService.getRiskMatrix(), "Matriz de riesgos configurada con exito"));
    }

    @Override
    @PostMapping("/{proyectoId}/riesgos")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoCreatedResponseDTO>> crearRiesgo(
            @PathVariable String proyectoId,
            @Valid @RequestBody RiesgoRequestDTO requestDto) {
        RiesgoCreatedResponseDTO response = riesgoService.createRisk(proyectoId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, response.mensaje()));
    }

    @Override
    @PutMapping("/{proyectoId}/riesgos/{riesgoId}")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<RiesgoResponseDTO>> actualizarRiesgo(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @Valid @RequestBody RiesgoRequestDTO requestDto) {
        RiesgoResponseDTO response = riesgoService.updateRisk(proyectoId, riesgoId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "Riesgo actualizado con exito"));
    }

    @Override
    @DeleteMapping("/{proyectoId}/riesgos/{riesgoId}")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<Void> eliminarRiesgo(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId) {
        riesgoService.deleteRisk(proyectoId, riesgoId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{proyectoId}/riesgos/{riesgoId}/tratamiento")
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:EDITAR', #proyectoId, authentication)")
    public ResponseEntity<ApiResponse<Void>> verificarTratamiento(
            @PathVariable String proyectoId,
            @PathVariable Integer riesgoId,
            @RequestBody String verificacion) {
        riesgoService.verificarTratamiento(proyectoId, riesgoId, verificacion);
        return ResponseEntity.ok(ApiResponse.success(null, "Tratamiento verificado y riesgo actualizado"));
    }
}
