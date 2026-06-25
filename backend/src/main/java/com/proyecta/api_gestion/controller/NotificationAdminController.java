package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.notification.*;
import com.proyecta.api_gestion.model.notification.NotificationPreference;
import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.repository.notification.NotificationEventCatalogRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.notification.NotificationPreferenceService;
import com.proyecta.api_gestion.service.notification.NotificationSenderPort;
import com.proyecta.api_gestion.service.notification.NotificationTemplateRenderer;
import com.proyecta.api_gestion.service.notification.NotificationTemplateService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notificaciones")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class NotificationAdminController {
    private static final Logger log = LoggerFactory.getLogger(NotificationAdminController.class);

    private final NotificationEventCatalogRepository eventCatalogRepository;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateRenderer renderer;
    private final NotificationSenderPort notificationSender;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final KeycloakIdentityExtractor identityExtractor;

    public NotificationAdminController(NotificationEventCatalogRepository eventCatalogRepository,
                                       NotificationTemplateService templateService,
                                       NotificationPreferenceService preferenceService,
                                       NotificationTemplateRenderer renderer,
                                       NotificationSenderPort notificationSender,
                                       SeguridadUsuarioRepository usuarioRepository,
                                       KeycloakIdentityExtractor identityExtractor) {
        this.eventCatalogRepository = eventCatalogRepository;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
        this.notificationSender = notificationSender;
        this.usuarioRepository = usuarioRepository;
        this.identityExtractor = identityExtractor;
    }

    @GetMapping("/eventos")
    public ResponseEntity<ApiResponse<List<NotificationEventCatalogDTO>>> listEvents() {
        var data = eventCatalogRepository.findAll().stream().map(event ->
                new NotificationEventCatalogDTO(event.getCode(), event.getName(), event.getDescription(), event.getCategory(), event.getDefaultEnabled(), event.getActive(), event.getRequiresProjectContext())
        ).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Eventos de notificacion listados correctamente"));
    }

    @GetMapping("/plantillas")
    public ResponseEntity<ApiResponse<List<NotificationTemplateDTO>>> listTemplates() {
        var data = templateService.listAll().stream().map(this::toDto).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Plantillas listadas correctamente"));
    }

    @PutMapping("/plantillas")
    public ResponseEntity<ApiResponse<NotificationTemplateDTO>> saveTemplate(@Valid @RequestBody NotificationTemplateUpdateRequest request,
                                                                             Authentication authentication) {
        NotificationTemplate template = new NotificationTemplate();
        template.setEventCode(request.eventCode());
        template.setEnabled(request.enabled());
        template.setHtmlEnabled(request.htmlEnabled());
        template.setSeverity(request.severity());
        template.setScope(request.scope());
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
        template.setTargetRoles(request.targetRoles());
        var saved = templateService.upsert(template, identityExtractor.resolveUsername(authentication));
        return ResponseEntity.ok(ApiResponse.success(toDto(saved), "Plantilla guardada correctamente"));
    }

    @GetMapping("/preferencias")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDTO>>> listPreferences() {
        var data = preferenceService.listAll().stream().map(this::toPreferenceDto).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Preferencias listadas correctamente"));
    }

    @PutMapping("/preferencias")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> savePreference(@Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        NotificationPreference saved = preferenceService.upsert(
                request.username(),
                request.eventCode(),
                request.projectId(),
                Boolean.TRUE.equals(request.enabled()),
                Boolean.TRUE.equals(request.emailEnabled())
        );
        return ResponseEntity.ok(ApiResponse.success(toPreferenceDto(saved), "Preferencia guardada correctamente"));
    }

    @PostMapping("/plantillas/preview")
    public ResponseEntity<ApiResponse<NotificationTemplatePreviewResponse>> preview(@RequestBody NotificationTemplatePreviewRequest request) {
        var subject = renderer.apply(request.subjectTemplate(), request.variables());
        var body = renderer.apply(request.bodyTemplate(), request.variables());
        return ResponseEntity.ok(ApiResponse.success(new NotificationTemplatePreviewResponse(subject, body, request.htmlEnabled()), "Preview generado correctamente"));
    }

    @PostMapping("/plantillas/test-send")
    public ResponseEntity<ApiResponse<String>> testSend(@Valid @RequestBody NotificationTestSendRequest request,
                                                        Authentication authentication) {
        try {
            String username = identityExtractor.resolveUsername(authentication);
            var userOpt = usuarioRepository.findByUsernameIgnoreCase(username);
            if (userOpt.isEmpty() || userOpt.get().getCorreo() == null || userOpt.get().getCorreo().isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.success("No se encontró correo electrónico para el usuario " + username, "USER_NO_EMAIL"));
            }

            String recipientEmail = userOpt.get().getCorreo();
            var subject = renderer.apply(request.subjectTemplate(), request.variables());
            var body = renderer.apply(request.bodyTemplate(), request.variables());
            var message = new com.proyecta.api_gestion.service.notification.NotificationMessage(subject, body, Boolean.TRUE.equals(request.htmlEnabled()));

            notificationSender.send(recipientEmail, message);
            log.info("Test notification email sent to {} (user: {})", recipientEmail, username);
            return ResponseEntity.ok(ApiResponse.success("Correo de prueba enviado correctamente a " + recipientEmail));
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(ApiResponse.success("Error al enviar el correo de prueba: " + e.getMessage(), "EMAIL_SEND_FAILED"));
        }
    }

    private NotificationTemplateDTO toDto(NotificationTemplate template) {
        return new NotificationTemplateDTO(
                template.getEventCode(),
                template.getEnabled(),
                template.getHtmlEnabled(),
                template.getSeverity(),
                template.getScope(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.getTargetRoles(),
                template.getUpdatedBy()
        );
    }

    private NotificationPreferenceDTO toPreferenceDto(NotificationPreference pref) {
        return new NotificationPreferenceDTO(
                pref.getId(),
                pref.getUser() != null ? pref.getUser().getUsername() : null,
                pref.getEventCode(),
                pref.getProjectId(),
                pref.getEnabled(),
                pref.getEmailEnabled()
        );
    }
}
