package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.Furag;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Módulo 2 — Proyectos", description = "Endpoints para la gestión de proyectos TIC")
@CrossOrigin(origins = "*")
public class ProyectoController implements com.proyecta.api_gestion.controller.interfaces.IProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @Override
    public ResponseEntity<ApiResponse<Page<ProyectoListDTO>>> listarProyectos(
            String nombre, String codigo, String dependencia, EstadoProyecto estado, Boolean peti,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<ProyectoListDTO> page = proyectoService.listarProyectos(nombre, codigo, dependencia, estado, peti, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Proyectos listados con éxito"));
    }

    @Override
    public ResponseEntity<ApiResponse<ProyectoResponseDTO>> obtenerProyecto(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerPorId(id), "Detalle del proyecto obtenido"));
    }

    @Override
    public ResponseEntity<ApiResponse<ProyectoCreatedDTO>> crearProyecto(@Valid @RequestBody ProyectoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(proyectoService.crearProyecto(dto), "Proyecto creado exitosamente"));
    }

    @Override
    public ResponseEntity<ApiResponse<ProyectoResponseDTO>> actualizarProyecto(
            @PathVariable String id,
            @Valid @RequestBody ProyectoUpdateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.actualizarProyecto(id, dto), "Proyecto actualizado exitosamente"));
    }

    @Override
    public ResponseEntity<ApiResponse<DashboardDTO>> obtenerDashboard() {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerDashboard(), "Métricas del dashboard obtenidas"));
    }

    @Override
    public ResponseEntity<Void> eliminarProyecto(String id) {
        proyectoService.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ApiResponse<ProyectoResumenDTO>> obtenerResumen(String id) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerResumen(id), "Resumen del proyecto obtenido"));
    }

    @Override
    public ResponseEntity<Void> cerrarProyecto(String id) {
        proyectoService.cerrarProyecto(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<ApiResponse<Furag>> obtenerFurag(String id) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerFurag(id), "FURAG obtenido"));
    }

    @Override
    public ResponseEntity<Void> actualizarFurag(String id, Furag furag) {
        proyectoService.actualizarFurag(id, furag);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/recalcular-avances")
    public ResponseEntity<ApiResponse<Void>> recalcularAvances() {
        proyectoService.recalcularAvances();
        return ResponseEntity.ok(ApiResponse.success("Avances recalculados exitosamente"));
    }
}