package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.notification.InAppNotificationDTO;
import com.proyecta.api_gestion.service.notification.InAppNotificationService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.proyecta.api_gestion.adapter.in.web.PageSupport;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notificaciones")
@Tag(name = "Notificaciones In-App", description = "Endpoints de notificaciones dentro de la aplicación")
@PreAuthorize("@localUserAuthorization.hasBaseAccess(authentication)")
public class InAppNotificationController {
    private final InAppNotificationService service;
    private final KeycloakIdentityExtractor identityExtractor;

    public InAppNotificationController(InAppNotificationService service, KeycloakIdentityExtractor identityExtractor) {
        this.service = service;
        this.identityExtractor = identityExtractor;
    }

    @Operation(
        summary = "Listar notificaciones in-app del usuario",
        description = "Retorna las notificaciones del usuario autenticado en forma paginada, con filtros opcionales por estado de lectura y por código de evento."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InAppNotificationDTO>>> list(
            Authentication authentication,
            @Parameter(description = "Filtra por estado leído (true/false)") @RequestParam(required = false) Boolean leido,
            @Parameter(description = "Filtra por código del evento de notificación")             @RequestParam(required = false) String eventCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) List<String> sort) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                PageSupport.toPage(service.list(username, leido, eventCode, PageSupport.fromParams(page, size, sort, 20))),
                "Notificaciones consultadas correctamente"));
    }

    @Operation(summary = "Contar notificaciones no leídas")
    @GetMapping("/no-leidas")
    public ResponseEntity<ApiResponse<Long>> unread(Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        return ResponseEntity.ok(ApiResponse.success(service.countUnread(username), "Cantidad de notificaciones no leidas"));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PatchMapping("/{id}/leer")
    public ResponseEntity<ApiResponse<Void>> read(@Parameter(description = "Identificador de la notificación") @PathVariable Long id, Authentication authentication) {
        // CWE-639: solo el dueno de la notificacion puede marcarla como leida.
        String username = identityExtractor.resolveUsername(authentication);
        service.markRead(id, username);
        return ResponseEntity.ok(ApiResponse.success("Notificacion marcada como leida"));
    }

    @Operation(summary = "Marcar todas las notificaciones del usuario como leídas")
    @PatchMapping("/marcar-todas-leidas")
    public ResponseEntity<ApiResponse<Void>> readAll(Authentication authentication) {
        String username = identityExtractor.resolveUsername(authentication);
        service.markAllRead(username);
        return ResponseEntity.ok(ApiResponse.success("Todas las notificaciones marcadas como leidas"));
    }
}
