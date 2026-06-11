package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.proyecto.DashboardDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCompletarInformacionDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCompletionStatusDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCreateDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoCreatedDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoListDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoRegistroInicialDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoResponseDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoResumenDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoUpdateDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.exception.BadRequestException;
import com.proyecta.api_gestion.model.Furag;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.service.interfaces.ProyectoService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Módulo 2 — Proyectos", description = "Endpoints para la gestión de proyectos TIC")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProyectoController implements com.proyecta.api_gestion.controller.interfaces.IProyectoController {

    private final ProyectoService proyectoService;
    private final KeycloakIdentityExtractor identityExtractor;

    public ProyectoController(ProyectoService proyectoService, KeycloakIdentityExtractor identityExtractor) {
        this.proyectoService = proyectoService;
        this.identityExtractor = identityExtractor;
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<Page<ProyectoListDTO>>> listarProyectos(
            String nombre, String codigo, String dependencia, EstadoProyecto estado, Boolean peti,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<ProyectoListDTO> page = proyectoService.listarProyectos(nombre, codigo, dependencia, estado, peti, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Proyectos listados con éxito"));
    }

    @Override
    @GetMapping("/mis-proyectos")
    @PreAuthorize("@proyectoSecurity.canAccessOwnProjects(authentication)")
    public ResponseEntity<ApiResponse<List<ProyectoListDTO>>> listarMisProyectos(Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        List<ProyectoListDTO> proyectos = proyectoService.listarProyectosAsignados(username);
        return ResponseEntity.ok(ApiResponse.success(proyectos, "Proyectos asignados listados con éxito"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<ApiResponse<ProyectoResponseDTO>> obtenerProyecto(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerPorId(id), "Detalle del proyecto obtenido"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:CREAR', authentication)")
    public ResponseEntity<ApiResponse<ProyectoCreatedDTO>> crearProyecto(@Valid @RequestBody ProyectoCreateDTO dto) {
        throw new BadRequestException("La creacion completa de proyectos esta deshabilitada. Use /api/v1/proyectos/registro-inicial.");
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:CREAR', authentication)")
    public ResponseEntity<ApiResponse<ProyectoCreatedDTO>> registrarProyectoInicial(
            @Valid @RequestBody ProyectoRegistroInicialDTO dto,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        ProyectoCreatedDTO creado = proyectoService.registrarProyectoInicial(dto, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(creado, "Proyecto registrado inicialmente"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:CREAR', authentication)")
    public ResponseEntity<ApiResponse<List<SeguridadUsuarioDTO>>> listarDirectoresAsignables() {
        return ResponseEntity.ok(ApiResponse.success(
                proyectoService.listarDirectoresAsignables(),
                "Directores asignables listados correctamente"
        ));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<ApiResponse<ProyectoCompletionStatusDTO>> obtenerEstadoCompletitud(
            @PathVariable String id,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                proyectoService.obtenerEstadoCompletitud(id, username),
                "Estado de completitud obtenido"
        ));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canCompleteInitialRegistration(#id, authentication)")
    public ResponseEntity<ApiResponse<ProyectoResponseDTO>> completarInformacionInicial(
            @PathVariable String id,
            @Valid @RequestBody ProyectoCompletarInformacionDTO dto,
            Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                proyectoService.completarInformacionInicial(id, dto, username),
                "Informacion inicial completada exitosamente"
        ));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:EDITAR', #id, authentication)")
    public ResponseEntity<ApiResponse<ProyectoResponseDTO>> actualizarProyecto(
            @PathVariable String id,
            @Valid @RequestBody ProyectoUpdateDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.actualizarProyecto(id, dto), "Proyecto actualizado exitosamente"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('PROYECTO:VER', authentication)")
    public ResponseEntity<ApiResponse<DashboardDTO>> obtenerDashboard() {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerDashboard(), "Métricas del dashboard obtenidas"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccess('PROYECTO:EDITAR', #id, authentication)")
    public ResponseEntity<Void> eliminarProyecto(String id) {
        proyectoService.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<ApiResponse<ProyectoResumenDTO>> obtenerResumen(String id) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerResumen(id), "Resumen del proyecto obtenido"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:CERRAR', #id, authentication)")
    public ResponseEntity<Void> cerrarProyecto(String id) {
        proyectoService.cerrarProyecto(id);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:VER', #id, authentication)")
    public ResponseEntity<ApiResponse<Furag>> obtenerFurag(String id) {
        return ResponseEntity.ok(ApiResponse.success(proyectoService.obtenerFurag(id), "FURAG obtenido"));
    }

    @Override
    @PreAuthorize("@proyectoSecurity.canAccessOperational('PROYECTO:EDITAR', #id, authentication)")
    public ResponseEntity<Void> actualizarFurag(String id, Furag furag) {
        proyectoService.actualizarFurag(id, furag);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/recalcular-avances")
    @PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
    public ResponseEntity<ApiResponse<Void>> recalcularAvances() {
        proyectoService.recalcularAvances();
        return ResponseEntity.ok(ApiResponse.success("Avances recalculados exitosamente"));
    }
}
