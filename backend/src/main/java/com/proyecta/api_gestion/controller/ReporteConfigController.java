package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.report.ReporteConfigUpdateDTO;
import com.proyecta.api_gestion.model.ReporteConfig;
import com.proyecta.api_gestion.repository.ReporteConfigRepository;
import com.proyecta.api_gestion.config.openapi.StandardApiResponses;
import com.proyecta.api_gestion.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion/reportes")
@CrossOrigin(origins = "*")
@Tag(name = "Configuración de Reportes", description = "Endpoints para administrar los tipos de reportes disponibles en el sistema.")
@StandardApiResponses
public class ReporteConfigController {

    private final ReporteConfigRepository repository;

    public ReporteConfigController(ReporteConfigRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Listar todas las configuraciones de reportes", description = "Obtiene todos los reportes, incluyendo los inactivos.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReporteConfig>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(repository.findAll(), "Configuraciones obtenidas con éxito"));
    }

    @Operation(summary = "Actualizar configuración de un reporte", description = "Permite cambiar el nombre, descripción, orden o estado de un reporte.")
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ReporteConfig>> update(@PathVariable String id, @RequestBody ReporteConfigUpdateDTO dto) {
        ReporteConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración de reporte no encontrada: " + id));

        if (dto.nombre() != null) config.setNombre(dto.nombre());
        if (dto.descripcion() != null) config.setDescripcion(dto.descripcion());
        if (dto.orden() != null) config.setOrden(dto.orden());
        if (dto.activo() != null) config.setActivo(dto.activo());

        return ResponseEntity.ok(ApiResponse.success(repository.save(config), "Configuración actualizada con éxito"));
    }
}
