package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.controller.interfaces.IAvanceProyectoController;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.dashboard.ProyectoAvanceDetalleDTO;
import com.proyecta.api_gestion.service.interfaces.ProyectoAvanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class AvanceProyectoController implements IAvanceProyectoController {

    private final ProyectoAvanceService proyectoAvanceService;

    public AvanceProyectoController(ProyectoAvanceService proyectoAvanceService) {
        this.proyectoAvanceService = proyectoAvanceService;
    }

    @Override
    @GetMapping("/{proyectoId}/avance")
    public ResponseEntity<ApiResponse<ProyectoAvanceDetalleDTO>> getAvanceProyecto(
            @PathVariable String proyectoId) {
        ProyectoAvanceDetalleDTO detalle = proyectoAvanceService.obtenerAvanceDetallado(proyectoId);
        return ResponseEntity.ok(ApiResponse.success(detalle, "Avance del proyecto obtenido con éxito"));
    }
}
