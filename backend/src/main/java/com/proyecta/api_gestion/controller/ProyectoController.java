package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.Furag;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @GetMapping
    @Operation(summary = "EP-PROY-01 · Listar proyectos", description = "Listar todos los proyectos con filtros y paginación.")
    public ResponseEntity<Page<ProyectoListDTO>> listarProyectos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String dependencia,
            @RequestParam(required = false) EstadoProyecto estado,
            @RequestParam(required = false) Boolean peti,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(proyectoService.listarProyectos(nombre, codigo, dependencia, estado, peti, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "EP-PROY-02 · Obtener detalle", description = "Obtener detalle completo de un proyecto por ID.")
    public ResponseEntity<ProyectoResponseDTO> obtenerProyecto(@PathVariable String id) {
        return ResponseEntity.ok(proyectoService.obtenerPorId(id));
    }

    @PostMapping
    @Operation(summary = "EP-PROY-03 · Crear proyecto", description = "Crear un nuevo proyecto TIC (wizard completo).")
    public ResponseEntity<ProyectoCreatedDTO> crearProyecto(@Valid @RequestBody ProyectoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proyectoService.crearProyecto(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "EP-PROY-04 · Actualizar proyecto", description = "Actualizar datos editables de un proyecto existente.")
    public ResponseEntity<ProyectoResponseDTO> actualizarProyecto(
            @PathVariable String id,
            @Valid @RequestBody ProyectoUpdateDTO dto) {
        return ResponseEntity.ok(proyectoService.actualizarProyecto(id, dto));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "EP-PROY-05 · Dashboard", description = "Métricas resumidas para el dashboard principal.")
    public ResponseEntity<DashboardDTO> obtenerDashboard() {
        return ResponseEntity.ok(proyectoService.obtenerDashboard());
    }

    @Override
    public ResponseEntity<Void> eliminarProyecto(String id) {
        proyectoService.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProyectoResumenDTO> obtenerResumen(String id) {
        return ResponseEntity.ok(proyectoService.obtenerResumen(id));
    }

    @Override
    public ResponseEntity<Void> cerrarProyecto(String id) {
        proyectoService.cerrarProyecto(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Furag> obtenerFurag(String id) {
        return ResponseEntity.ok(proyectoService.obtenerFurag(id));
    }

    @Override
    public ResponseEntity<Void> actualizarFurag(String id, Furag furag) {
        proyectoService.actualizarFurag(id, furag);
        return ResponseEntity.ok().build();
    }
}