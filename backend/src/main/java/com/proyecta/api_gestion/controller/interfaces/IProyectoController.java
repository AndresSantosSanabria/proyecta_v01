package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Módulo 2 — Proyectos", description = "Endpoints para la gestión de proyectos TIC")
public interface IProyectoController {

    @Operation(summary = "EP-PROY-01 · Listar proyectos", description = "Listar todos los proyectos con filtros y paginación.")
    @GetMapping
    ResponseEntity<Page<ProyectoListDTO>> listarProyectos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String dependencia,
            @RequestParam(required = false) EstadoProyecto estado,
            @RequestParam(required = false) Boolean peti,
            Pageable pageable);

    @Operation(summary = "EP-PROY-02 · Obtener detalle", description = "Obtener detalle completo de un proyecto por ID.")
    @GetMapping("/{id}")
    ResponseEntity<ProyectoResponseDTO> obtenerProyecto(@PathVariable String id);

    @Operation(summary = "EP-PROY-03 · Crear proyecto", description = "Crear un nuevo proyecto TIC (wizard completo).")
    @PostMapping
    ResponseEntity<ProyectoCreatedDTO> crearProyecto(@Valid @RequestBody ProyectoCreateDTO dto);

    @Operation(summary = "EP-PROY-04 · Actualizar proyecto", description = "Actualizar datos editables de un proyecto existente.")
    @PutMapping("/{id}")
    ResponseEntity<ProyectoResponseDTO> actualizarProyecto(
            @PathVariable String id,
            @Valid @RequestBody ProyectoUpdateDTO dto);

    @Operation(summary = "EP-PROY-05 · Dashboard", description = "Métricas resumidas para el dashboard principal.")
    @GetMapping("/dashboard")
    ResponseEntity<DashboardDTO> obtenerDashboard();
}
