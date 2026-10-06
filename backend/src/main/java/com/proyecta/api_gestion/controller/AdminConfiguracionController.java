package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.security.SeguridadMatrizPermisosUpdateRequest;
import com.proyecta.api_gestion.dto.security.SeguridadPermisoDTO;
import com.proyecta.api_gestion.dto.security.SeguridadRolDTO;
import com.proyecta.api_gestion.dto.security.SeguridadRolRequest;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoDTO;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioProyectoRequest;
import com.proyecta.api_gestion.dto.security.SeguridadUsuarioUpdateRequest;
import com.proyecta.api_gestion.service.impl.FileStorageServiceImpl;
import com.proyecta.api_gestion.service.security.dynamic.SecurityAdministrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.proyecta.api_gestion.adapter.in.web.PageSupport;
import org.springframework.data.domain.Page;
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
@Tag(name = "Administración - Seguridad", description = "Endpoints de administración de usuarios, roles y permisos de la plataforma")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class AdminConfiguracionController {

    private final SecurityAdministrationService securityAdministrationService;
    private final FileStorageServiceImpl fileStorageService;

    public AdminConfiguracionController(SecurityAdministrationService securityAdministrationService,
                                         FileStorageServiceImpl fileStorageService) {
        this.securityAdministrationService = securityAdministrationService;
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Listar usuarios del sistema", description = "Lista los usuarios de la plataforma con paginación y sincroniza los datos del usuario autenticado desde Keycloak.")
    @GetMapping("/usuarios")
    public ResponseEntity<ApiResponse<Page<SeguridadUsuarioDTO>>> listarUsuarios(
            @Parameter(description = "Texto de búsqueda por nombre o correo") @RequestParam(required = false) String search,
            @Parameter(description = "Filtra por código de rol") @RequestParam(required = false) String rol,
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) List<String> sort) {
        securityAdministrationService.sincronizarUsuarioAutenticado(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                PageSupport.toPage(securityAdministrationService.listarUsuarios(
                        search, rol, PageSupport.fromParams(page, size, sort, 20))),
                "Usuarios listados correctamente"));
    }

    @Operation(summary = "Actualizar los datos de un usuario")
    @PutMapping("/usuarios")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadUsuarioDTO>> actualizarUsuario(
            @RequestBody SeguridadUsuarioUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.actualizarUsuario(request),
                "Usuario actualizado correctamente"));
    }

    @Operation(summary = "Listar roles activos del sistema")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<SeguridadRolDTO>>> listarRoles() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarRolesConPermisos(false),
                "Roles y permisos listados correctamente"));
    }

    @Operation(summary = "Listar todos los roles del sistema", description = "Incluye además los roles inactivos o desactivados.")
    @GetMapping("/roles/todos")
    public ResponseEntity<ApiResponse<List<SeguridadRolDTO>>> listarRolesTodos() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarRolesConPermisos(true),
                "Roles y permisos listados correctamente"));
    }

    @Operation(summary = "Crear un rol nuevo")
    @PostMapping("/roles")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadRolDTO>> crearRol(@RequestBody SeguridadRolRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.guardarRol(request),
                "Rol creado correctamente"));
    }

    @Operation(summary = "Actualizar un rol existente")
    @PutMapping("/roles/{codigo}")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadRolDTO>> actualizarRol(
            @Parameter(description = "Código único del rol") @PathVariable String codigo,
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

    @Operation(summary = "Desactivar un rol")
    @DeleteMapping("/roles/{codigo}")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadRolDTO>> desactivarRol(@Parameter(description = "Código único del rol") @PathVariable String codigo) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.eliminarRol(codigo),
                "Rol desactivado correctamente"));
    }

    @Operation(summary = "Listar permisos del sistema")
    @GetMapping("/permisos")
    public ResponseEntity<ApiResponse<List<SeguridadPermisoDTO>>> listarPermisos() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarPermisos(),
                "Permisos listados correctamente"));
    }

    @Operation(summary = "Listar cargos disponibles para asignación")
    @GetMapping("/cargos-asignacion")
    public ResponseEntity<ApiResponse<List<String>>> listarCargosAsignacion() {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarCargosAsignacion(),
                "Cargos de asignacion listados correctamente"));
    }

    @Operation(summary = "Actualizar la matriz de roles y permisos")
    @PutMapping("/roles-permisos")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> actualizarMatriz(@RequestBody SeguridadMatrizPermisosUpdateRequest request) {
        securityAdministrationService.actualizarMatriz(request);
        return ResponseEntity.ok(ApiResponse.success("Matriz de permisos actualizada correctamente"));
    }

    @Operation(summary = "Asignar un usuario a un proyecto")
    @PostMapping("/usuario-proyecto")
    @Transactional
    public ResponseEntity<ApiResponse<SeguridadUsuarioProyectoDTO>> asignarUsuarioProyecto(
            @RequestBody SeguridadUsuarioProyectoRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.asignarUsuarioProyecto(request),
                "Usuario asignado al proyecto correctamente"));
    }

    @Operation(summary = "Listar proyectos asignados a un usuario")
    @GetMapping("/usuario-proyecto")
    public ResponseEntity<ApiResponse<List<SeguridadUsuarioProyectoDTO>>> listarAsignaciones(
            @Parameter(description = "Nombre de usuario (username)") @RequestParam String username) {
        return ResponseEntity.ok(ApiResponse.success(
                securityAdministrationService.listarAsignaciones(username),
                "Asignaciones del usuario listadas correctamente"));
    }

    @Operation(summary = "Validar una ruta de almacenamiento de archivos")
    @GetMapping("/storage-path/test")
    public ResponseEntity<ApiResponse<FileStorageServiceImpl.StoragePathInfo>> testStoragePath(
            @Parameter(description = "Ruta del sistema a validar") @RequestParam String path) {
        FileStorageServiceImpl.StoragePathInfo info = fileStorageService.testStoragePath(path);
        return ResponseEntity.ok(ApiResponse.success(info, info.valid()
                ? "Ruta de almacenamiento validada correctamente"
                : "La ruta de almacenamiento no es valida"));
    }
}
