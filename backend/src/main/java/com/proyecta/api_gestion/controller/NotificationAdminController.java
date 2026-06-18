package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.notification.*;
import com.proyecta.api_gestion.model.notification.NotificationPreference;
import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.repository.notification.NotificationEventCatalogRepository;
import com.proyecta.api_gestion.service.notification.NotificationPreferenceService;
import com.proyecta.api_gestion.service.notification.NotificationTemplateRenderer;
import com.proyecta.api_gestion.service.notification.NotificationTemplateService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notificaciones")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class NotificationAdminController {
    private final NotificationEventCatalogRepository eventCatalogRepository;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateRenderer renderer;
    private final KeycloakIdentityExtractor identityExtractor;

    public NotificationAdminController(NotificationEventCatalogRepository eventCatalogRepository,
                                       NotificationTemplateService templateService,
                                       NotificationPreferenceService preferenceService,
                                       NotificationTemplateRenderer renderer,
                                       KeycloakIdentityExtractor identityExtractor) {
        this.eventCatalogRepository = eventCatalogRepository;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
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

    private NotificationTemplateDTO toDto(NotificationTemplate template) {
        return new NotificationTemplateDTO(
                template.getEventCode(),
                template.getEnabled(),
                template.getHtmlEnabled(),
                template.getSeverity(),
                template.getScope(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
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
