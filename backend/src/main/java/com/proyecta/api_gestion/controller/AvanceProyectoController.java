package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IAvanceProyectoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.avance.EntregableConformidadResponseDTO;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/proyectos")
@CrossOrigin(origins = "*")
public class AvanceProyectoController implements IAvanceProyectoController {

    private final ProyectoAvanceService proyectoAvanceService;

    public AvanceProyectoController(ProyectoAvanceService proyectoAvanceService) {
        this.proyectoAvanceService = proyectoAvanceService;
    }

    @Override
    @GetMapping("/{proyectoId}/avance")
    public ResponseEntity<ApiResponse<ProyectoAvanceResponseDTO>> getAvanceProyecto(
            @PathVariable String proyectoId) {
        ProyectoAvanceResponseDTO detalle = proyectoAvanceService.obtenerAvanceDetallado(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(detalle, "Avance del proyecto obtenido con éxito"));
    }

    @Override
    @PatchMapping(value = "/{proyectoId}/avance/entregables/{entregableId}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<EntregableConformidadResponseDTO>> marcarConformidad(
            @PathVariable String proyectoId,
            @PathVariable Integer entregableId,
            @RequestParam Boolean conformidad,
            @RequestParam LocalDate fechaEntrega,
            @RequestPart MultipartFile evidencia) {
        
        EntregableConformidadResponseDTO result = proyectoAvanceService.actualizarConformidad(proyectoId, entregableId, conformidad, fechaEntrega, evidencia);
        return ResponseEntity.ok(ApiResponse.success(result, "Entregable marcado a conformidad exitosamente"));
    }
}
