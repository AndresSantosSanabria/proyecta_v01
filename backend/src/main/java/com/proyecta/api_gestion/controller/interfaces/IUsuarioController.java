package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.user.UsuarioDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Tag(name = "Módulo 8 — Usuarios y Equipo", description = "Endpoints para la gestión de usuarios y perfiles")
public interface IUsuarioController {

    @Operation(summary = "Obtener perfil del usuario autenticado", description = "Retorna los datos del usuario logueado en la sesión.")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<UsuarioDTO>> getMe();
}
