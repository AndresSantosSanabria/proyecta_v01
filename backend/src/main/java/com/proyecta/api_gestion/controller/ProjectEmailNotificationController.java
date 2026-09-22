package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.notification.ProjectEmailNotificationRequest;
import com.proyecta.api_gestion.service.notification.NotificacionEmailService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/proyectos")
@Tag(name = "Notificaciones por correo", description = "Envío de correos con hilo por proyecto")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class ProjectEmailNotificationController {

    private final NotificacionEmailService notificacionEmailService;
    private final KeycloakIdentityExtractor identityExtractor;

    public ProjectEmailNotificationController(
            NotificacionEmailService notificacionEmailService,
            KeycloakIdentityExtractor identityExtractor) {
        this.notificacionEmailService = notificacionEmailService;
        this.identityExtractor = identityExtractor;
    }

    @PostMapping("/{id}/notificaciones/email")
    @PreAuthorize("@proyectoSecurity.canSendDirectorNotification(#id, authentication)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendThreadedEmail(
            @PathVariable String id,
            @Valid @RequestBody ProjectEmailNotificationRequest request,
            Authentication authentication) {
        String actor = identityExtractor.resolveUsername(authentication);
        String messageId = notificacionEmailService.sendProjectThreadedEmail(id, request.mensaje(), actor);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("messageId", messageId),
                "Correo enviado en el hilo del proyecto"));
    }
}
