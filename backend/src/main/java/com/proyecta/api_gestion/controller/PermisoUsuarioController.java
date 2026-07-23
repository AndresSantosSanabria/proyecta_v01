package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixDTO;
import com.proyecta.api_gestion.dto.security.PermisoUsuarioMatrixUpdateRequest;
import com.proyecta.api_gestion.service.security.dynamic.PermisoUsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/configuracion/permisos-usuario")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class PermisoUsuarioController {

    private final PermisoUsuarioService permisoUsuarioService;

    public PermisoUsuarioController(PermisoUsuarioService permisoUsuarioService) {
        this.permisoUsuarioService = permisoUsuarioService;
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<ApiResponse<PermisoUsuarioMatrixDTO>> getMatrix(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(ApiResponse.success(
                permisoUsuarioService.getMatrix(usuarioId),
                "Matriz de permisos cargada correctamente"));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<ApiResponse<Void>> saveMatrix(@RequestBody PermisoUsuarioMatrixUpdateRequest request) {
        permisoUsuarioService.saveMatrix(request);
        return ResponseEntity.ok(ApiResponse.success("Matriz de permisos actualizada correctamente"));
    }
}
