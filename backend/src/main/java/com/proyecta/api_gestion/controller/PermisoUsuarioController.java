package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixDTO;
import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixUpdateRequest;
import com.proyecta.api_gestion.service.security.dynamic.PermisoUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/configuracion/permisos-usuario")
@Tag(name = "Administración - Permisos de Usuario", description = "Endpoints de asignación de permisos y roles por usuario")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class PermisoUsuarioController {

    private final PermisoUsuarioService permisoUsuarioService;

    public PermisoUsuarioController(PermisoUsuarioService permisoUsuarioService) {
        this.permisoUsuarioService = permisoUsuarioService;
    }

    @Operation(summary = "Obtener la matriz de permisos de un usuario")
    @GetMapping("/{usuarioId}")
    public ResponseEntity<ApiResponse<PermisoUsuarioMatrixDTO>> getMatrix(@Parameter(description = "Identificador del usuario") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(ApiResponse.success(
                permisoUsuarioService.getMatrix(usuarioId),
                "Matriz de permisos cargada correctamente"));
    }

    @Operation(
        summary = "Actualizar la matriz de permisos de un usuario",
        description = "Reemplaza los permisos y roles asignados al usuario indicado en la solicitud."
    )
    @PutMapping
    @Transactional
    public ResponseEntity<ApiResponse<Void>> saveMatrix(@RequestBody PermisoUsuarioMatrixUpdateRequest request) {
        permisoUsuarioService.saveMatrix(request);
        return ResponseEntity.ok(ApiResponse.success("Matriz de permisos actualizada correctamente"));
    }
}
