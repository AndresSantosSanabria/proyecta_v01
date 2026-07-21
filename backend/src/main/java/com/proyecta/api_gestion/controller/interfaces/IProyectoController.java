package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.proyecto.*;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Módulo 2 — Proyectos", description = "Endpoints para la gestión de proyectos TIC")
public interface IProyectoController {

    @Operation(summary = "EP-PROY-01 · Listar proyectos", description = "Listar todos los proyectos con filtros y paginación.")
    @GetMapping
    ResponseEntity<ApiResponse<Page<ProyectoListDTO>>> listarProyectos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String dependencia,
            @RequestParam(required = false) EstadoProyecto estado,
            @RequestParam(required = false) Boolean peti,
            Pageable pageable);

    @Operation(summary = "EP-PROY-01B · Listar mis proyectos", description = "Retorna los proyectos asignados al usuario autenticado.")
    @GetMapping("/mis-proyectos")
    ResponseEntity<ApiResponse<List<ProyectoListDTO>>> listarMisProyectos(Authentication authentication);

    @Operation(summary = "EP-PROY-01C - Directores asignables", description = "Lista usuarios activos con rol directivo para asignarlos como Director del proyecto.")
    @GetMapping("/directores-asignables")
    ResponseEntity<ApiResponse<List<SeguridadUsuarioDTO>>> listarDirectoresAsignables();

    @Operation(summary = "EP-PROY-02 · Obtener detalle", description = "Obtener detalle completo de un proyecto por ID.")
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ProyectoResponseDTO>> obtenerProyecto(@PathVariable String id);

    @Operation(summary = "EP-PROY-03 · Crear proyecto", description = "Crear un nuevo proyecto TIC (wizard completo).")
    @PostMapping
    ResponseEntity<ApiResponse<ProyectoCreatedDTO>> crearProyecto(@Valid @RequestBody ProyectoCreateDTO dto);

    @Operation(summary = "EP-PROY-03A - Siguiente codigo", description = "Retorna el siguiente codigo auto-generado para un nuevo proyecto con formato IS-PROY-CUN-YYYY-NNN.")
    @GetMapping("/siguiente-codigo")
    ResponseEntity<ApiResponse<String>> obtenerSiguienteCodigo();

    @Operation(summary = "EP-PROY-03B - Registro inicial", description = "Crear el registro minimo del proyecto y dejarlo pendiente de completar por el Director asignado.")
    @PostMapping("/registro-inicial")
    ResponseEntity<ApiResponse<ProyectoCreatedDTO>> registrarProyectoInicial(
            @Valid @RequestBody ProyectoRegistroInicialDTO dto,
            Authentication authentication);

    @Operation(summary = "EP-PROY-03C - Estado de completitud", description = "Consultar si el proyecto requiere completitud inicial del Director asignado.")
    @GetMapping("/{id}/completion-status")
    ResponseEntity<ApiResponse<ProyectoCompletionStatusDTO>> obtenerEstadoCompletitud(
            @PathVariable String id,
            Authentication authentication);

    @Operation(summary = "EP-PROY-03D - Completar informacion inicial", description = "Completar los datos restantes del proyecto pendiente. Solo aplica al Director asignado.")
    @PutMapping("/{id}/completar-informacion")
    ResponseEntity<ApiResponse<ProyectoResponseDTO>> completarInformacionInicial(
            @PathVariable String id,
            @Valid @RequestBody ProyectoCompletarInformacionDTO dto,
            Authentication authentication);

    @Operation(summary = "EP-PROY-04 · Actualizar proyecto", description = "Actualizar datos editables de un proyecto existente.")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<ProyectoResponseDTO>> actualizarProyecto(
            @PathVariable String id,
            @Valid @RequestBody ProyectoUpdateDTO dto,
            Authentication authentication);

    @Operation(summary = "EP-PROY-05 · Dashboard", description = "Métricas resumidas para el dashboard principal.")
    @GetMapping("/dashboard")
    ResponseEntity<ApiResponse<DashboardDTO>> obtenerDashboard();

    @Operation(summary = "EP-PROY-06 · Eliminar proyecto", description = "Eliminar un proyecto por ID.")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> eliminarProyecto(@PathVariable String id, Authentication authentication);

    @Operation(summary = "EP-PROY-07 · Resumen para cierre", description = "Obtener resumen ejecutivo para la pantalla de cierre.")
    @GetMapping("/{id}/resumen")
    ResponseEntity<ApiResponse<ProyectoResumenDTO>> obtenerResumen(@PathVariable String id);

    @Operation(summary = "EP-PROY-08 · Cerrar proyecto", description = "Cerrar formalmente un proyecto (requiere 100% avance).")
    @PatchMapping("/{id}/cerrar")
    ResponseEntity<Void> cerrarProyecto(@PathVariable String id);

    @Operation(summary = "EP-PROY-09 · Obtener FURAG", description = "Obtener respuestas FURAG del proyecto.")
    @GetMapping("/{id}/furag")
    ResponseEntity<ApiResponse<com.proyecta.api_gestion.model.Furag>> obtenerFurag(@PathVariable String id);

    @Operation(summary = "EP-PROY-10 · Actualizar FURAG", description = "Actualizar respuestas FURAG del proyecto.")
    @PutMapping("/{id}/furag")
    ResponseEntity<Void> actualizarFurag(@PathVariable String id, @RequestBody com.proyecta.api_gestion.model.Furag furag);

    @Operation(summary = "EP-PROY-11 · Recalcular avances", description = "Recalcular avances de todos los proyectos basado en estado de entregables.")
    @PostMapping("/recalcular-avances")
    ResponseEntity<ApiResponse<Void>> recalcularAvances();
}
