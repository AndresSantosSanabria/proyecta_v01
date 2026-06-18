package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.notification.InAppNotificationDTO;
import com.proyecta.api_gestion.service.notification.InAppNotificationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notificaciones")
public class InAppNotificationController {
    private final InAppNotificationService service;
    private final KeycloakIdentityExtractor identityExtractor;

    public InAppNotificationController(InAppNotificationService service, KeycloakIdentityExtractor identityExtractor) {
        this.service = service;
        this.identityExtractor = identityExtractor;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InAppNotificationDTO>>> list(Authentication authentication, Pageable pageable) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.list(username, pageable), "Notificaciones consultadas correctamente"));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<ApiResponse<Long>> unread(Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.countUnread(username), "Cantidad de notificaciones no leidas"));
    }

    @PatchMapping("/{id}/leer")
    public ResponseEntity<ApiResponse<Void>> read(@PathVariable Long id) {
        service.markRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notificacion marcada como leida"));
    }
}
