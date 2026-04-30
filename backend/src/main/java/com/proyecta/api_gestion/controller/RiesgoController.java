package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.risk.*;
import com.proyecta.api_gestion.model.enums.Impacto;
import com.proyecta.api_gestion.model.enums.Probabilidad;
import com.proyecta.api_gestion.service.IRiesgoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Matriz de Riesgos", description = "Endpoints para la identificación y mitigación de amenazas en proyectos")
public class RiesgoController {

    @Autowired
    private IRiesgoService riesgoService;

    @Operation(summary = "Listar matriz de riesgos de un proyecto", 
               description = "Retorna todos los riesgos asociados a un proyecto específico.")
    @GetMapping("/projects/{id}/risks")
    public ResponseEntity<ApiResponse<List<RiesgoResponseDTO>>> getProjectRisks(
            @Parameter(description = "ID del proyecto", example = "IS-PROY-2024-001") @PathVariable String id) {
        List<RiesgoResponseDTO> risks = riesgoService.getRisksByProject(id);
        return ResponseEntity.ok(ApiResponse.success(risks, "Matriz de riesgos recuperada con éxito"));
    }

    @Operation(summary = "Agregar un nuevo riesgo", 
               description = "Crea una amenaza. El proyectoId es obligatorio en el body.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Riesgo registrado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Datos inválidos")
    })
    @PostMapping("/risks")
    public ResponseEntity<ApiResponse<RiesgoResponseDTO>> createRisk(
            @Valid @RequestBody RiesgoCreateDTO createDto) {
        RiesgoResponseDTO createdRisk = riesgoService.createRisk(createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(createdRisk, "Riesgo registrado con éxito"));
    }

    @Operation(summary = "Editar un riesgo existente", 
               description = "Actualiza los datos de un riesgo. El id del riesgo es obligatorio en el body.")
    @PutMapping("/risks")
    public ResponseEntity<ApiResponse<RiesgoResponseDTO>> updateRisk(
            @Valid @RequestBody RiesgoUpdateDTO updateDto) {
        RiesgoResponseDTO updatedRisk = riesgoService.updateRisk(updateDto);
        return ResponseEntity.ok(ApiResponse.success(updatedRisk, "Riesgo actualizado con éxito"));
    }

    @Operation(summary = "Tratar un riesgo (Mitigación)", 
               description = "Cambia el estado a 'Tratado'. El id es obligatorio en el body.")
    @PatchMapping("/risks/treat")
    public ResponseEntity<ApiResponse<RiesgoResponseDTO>> treatRisk(
            @Valid @RequestBody RiesgoTratamientoDTO treatmentDto) {
        RiesgoResponseDTO treatedRisk = riesgoService.treatRisk(treatmentDto);
        return ResponseEntity.ok(ApiResponse.success(treatedRisk, "Riesgo marcado como TRATADO."));
    }

    @Schema(hidden = true)
    private Probabilidad _prob;
    @Schema(hidden = true)
    private Impacto _imp;
}
