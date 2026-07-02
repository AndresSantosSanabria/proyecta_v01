package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.user.GlobalNotificationUpdateRequest;
import com.proyecta.api_gestion.dto.user.UsuarioDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Módulo 8 — Usuarios y Equipo", description = "Endpoints para la gestión de usuarios y perfiles")
public interface IUsuarioController {

    @Operation(summary = "Obtener perfil del usuario autenticado", description = "Retorna los datos del usuario logueado en la sesión.")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<UsuarioDTO>> getMe(Jwt jwt, Authentication authentication);

    @Operation(summary = "Actualizar flag de notificaciones globales del usuario autenticado",
               description = "Activa o desactiva la recepción de notificaciones globales para el usuario actual.")
    @PatchMapping("/me/notificaciones-globales")
    ResponseEntity<ApiResponse<Boolean>> updateGlobalNotifications(
            @RequestBody GlobalNotificationUpdateRequest request,
            Authentication authentication);
}
