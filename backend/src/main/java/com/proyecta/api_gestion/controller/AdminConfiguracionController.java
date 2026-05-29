package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.security.SeguridadAutorizacionMeDTO;
import com.proyecta.api_gestion.dto.security.SeguridadMatrizPermisosUpdateRequest;
import com.proyecta.api_gestion.dto.security.SeguridadPermisoDTO;
import com.proyecta.api_gestion.dto.security.SeguridadRolDTO;
import com.proyecta.api_gestion.dto.security.SeguridadRolRequest;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoRequest;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioUpdateRequest;
import com.proyecta.api_gestion.dto.security.SystemParameterDTO;
import com.proyecta.api_gestion.dto.security.SystemParameterUpsertRequest;
import com.proyecta.api_gestion.service.security.dynamic.SecurityAdministrationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configuracion")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class AdminConfiguracionController {

    private final SecurityAdministrationService securityAdministrationService;

    public AdminConfiguracionController(SecurityAdministrationService securityAdministrationService) {
        this.securityAdministrationService = securityAdministrationService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<Page<SeguridadUsuarioDTO>>> listarUsuarios(
            @RequestParam(required = false) String search,
            Authentication authentication,
            Pageable pageable) {
        securityAdministrationService.sincronizarUsuarioAutenticado(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarUsuarios(search, pageable),
                "Usuarios listados correctamente"));
    }

    @PutMapping("/usuarios")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadUsuarioDTO>> actualizarUsuario(
            @RequestBody SeguridadUsuarioUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.actualizarUsuario(request),
                "Usuario actualizado correctamente"));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<SeguridadRolDTO>>> listarRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarRolesConPermisos(false),
                "Roles y permisos listados correctamente"));
    }

    @GetMapping("/roles/todos")
    public ResponseEntity<ApiResponse<List<SeguridadRolDTO>>> listarRolesTodos() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarRolesConPermisos(true),
                "Roles y permisos listados correctamente"));
    }

    @PostMapping("/roles")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadRolDTO>> crearRol(@RequestBody SeguridadRolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.guardarRol(request),
                "Rol creado correctamente"));
    }

    @PutMapping("/roles/{codigo}")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadRolDTO>> actualizarRol(
            @PathVariable String codigo,
            @RequestBody SeguridadRolRequest request) {
        SeguridadRolRequest payload = new SeguridadRolRequest(
                codigo,
                request.nombre(),
                request.descripcion(),
                request.transversal(),
                request.activo());
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.guardarRol(payload),
                "Rol actualizado correctamente"));
    }

    @DeleteMapping("/roles/{codigo}")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadRolDTO>> desactivarRol(@PathVariable String codigo) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.eliminarRol(codigo),
                "Rol desactivado correctamente"));
    }

    @GetMapping("/permisos")
    public ResponseEntity<ApiResponse<List<SeguridadPermisoDTO>>> listarPermisos() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarPermisos(),
                "Permisos listados correctamente"));
    }

    @GetMapping("/cargos-asignacion")
    public ResponseEntity<ApiResponse<List<String>>> listarCargosAsignacion() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarCargosAsignacion(),
                "Cargos de asignacion listados correctamente"));
    }

    @GetMapping("/parametros")
    public ResponseEntity<ApiResponse<List<SystemParameterDTO>>> listarParametrosSistema() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarParametrosSistema(),
                "Parametros del sistema listados correctamente"));
    }

    @PutMapping("/parametros")
    @Transactional
    public ResponseEntity<ApiResponse<SystemParameterDTO>> guardarParametroSistema(
            @RequestBody SystemParameterUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.guardarParametroSistema(request),
                "Parametro del sistema guardado correctamente"));
    }

    @DeleteMapping("/parametros/{key}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> eliminarParametroSistema(@PathVariable String key) {
        securityAdministrationService.eliminarParametroSistema(key);
        return ResponseEntity.ok(ApiResponse.success("Parametro del sistema eliminado correctamente"));
    }

    @PutMapping("/roles-permisos")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> actualizarMatriz(@RequestBody SeguridadMatrizPermisosUpdateRequest request) {
        securityAdministrationService.actualizarMatriz(request);
        return ResponseEntity.ok(ApiResponse.success("Matriz de permisos actualizada correctamente"));
    }

    @PostMapping("/usuario-proyecto")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadUsuarioProyectoDTO>> asignarUsuarioProyecto(
            @RequestBody SeguridadUsuarioProyectoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.asignarUsuarioProyecto(request),
                "Usuario asignado al proyecto correctamente"));
    }

    @GetMapping("/usuario-proyecto")
    public ResponseEntity<ApiResponse<List<SeguridadUsuarioProyectoDTO>>> listarAsignaciones(
            @RequestParam String username) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarAsignaciones(username),
                "Asignaciones del usuario listadas correctamente"));
    }
}
