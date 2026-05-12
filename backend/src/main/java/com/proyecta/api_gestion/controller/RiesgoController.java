package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IRiesgoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.service.IRiesgoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
public class RiesgoController implements IRiesgoController {

    private final IRiesgoService riesgoService;

    public RiesgoController(IRiesgoService riesgoService) {
        this.riesgoService = riesgoService;
    }

    @Override
    @GetMapping("/{proyectoId}/riesgos")
    public ResponseEntity<ApiResponse<RiesgoListResponseDTO>> listarRiesgos(@PathVariable String proyectoId) {
        RiesgoListResponseDTO riesgos = riesgoService.getRisksByProject(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(riesgos, "Matriz de riesgos recuperada con éxito"));
    }

    @Override
    @PostMapping("/{proyectoId}/riesgos")
    public ResponseEntity<ApiResponse<RiesgoCreatedResponseDTO>> crearRiesgo(
            @PathVariable String proyectoId, 
            @Valid @RequestBody RiesgoRequestDTO requestDto) {
        RiesgoCreatedResponseDTO response = riesgoService.createRisk(proyectoId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, response.mensaje()));
    }

    @Override
    @PutMapping("/{proyectoId}/riesgos/{riesgoId}")
    public ResponseEntity<ApiResponse<RiesgoResponseDTO>> actualizarRiesgo(
            @PathVariable String proyectoId, 
            @PathVariable Integer riesgoId, 
            @Valid @RequestBody RiesgoRequestDTO requestDto) {
        RiesgoResponseDTO response = riesgoService.updateRisk(proyectoId, riesgoId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "Riesgo actualizado con éxito"));
    }

    @Override
    @DeleteMapping("/{proyectoId}/riesgos/{riesgoId}")
    public ResponseEntity<Void> eliminarRiesgo(
            @PathVariable String proyectoId, 
            @PathVariable Integer riesgoId) {
        riesgoService.deleteRisk(proyectoId, riesgoId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{proyectoId}/riesgos/{riesgoId}/tratamiento")
    public ResponseEntity<ApiResponse<Void>> verificarTratamiento(
            @PathVariable String proyectoId, 
            @PathVariable Integer riesgoId, 
            @RequestBody String verificacion) {
        riesgoService.verificarTratamiento(proyectoId, riesgoId, verificacion);
        return ResponseEntity.ok(ApiResponse.success(null, "Tratamiento verificado y riesgo actualizado"));
    }
}
