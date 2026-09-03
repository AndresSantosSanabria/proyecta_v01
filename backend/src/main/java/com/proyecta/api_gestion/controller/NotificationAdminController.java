package com.proyecta.api_gestion.controller;

import com.proyecta.api_gestion.dto.common.ApiResponse;
import com.proyecta.api_gestion.dto.notification.NotificationAuditDTO;
import com.proyecta.api_gestion.dto.notification.NotificationDispatchLogDTO;
import com.proyecta.api_gestion.dto.notification.NotificationEventCatalogDTO;
import com.proyecta.api_gestion.dto.notification.NotificationPreferenceDTO;
import com.proyecta.api_gestion.dto.notification.NotificationPreferenceUpdateRequest;
import com.proyecta.api_gestion.dto.notification.NotificationTemplateDTO;
import com.proyecta.api_gestion.dto.notification.NotificationTemplatePreviewRequest;
import com.proyecta.api_gestion.dto.notification.NotificationTemplatePreviewResponse;
import com.proyecta.api_gestion.dto.notification.NotificationTemplateUpdateRequest;
import com.proyecta.api_gestion.dto.notification.NotificationTestSendRequest;
import com.proyecta.api_gestion.model.notification.NotificationAudit;
import com.proyecta.api_gestion.model.notification.NotificationEventCatalog;
import com.proyecta.api_gestion.model.notification.NotificationMailDispatchLog;
import com.proyecta.api_gestion.model.notification.NotificationPreference;
import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.notification.NotificationAuditRepository;
import com.proyecta.api_gestion.repository.notification.NotificationEventCatalogRepository;
import com.proyecta.api_gestion.repository.notification.NotificationMailDispatchLogRepository;
import com.proyecta.api_gestion.repository.notification.NotificationTemplateRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.notification.NotificationPreferenceService;
import com.proyecta.api_gestion.service.notification.NotificationSenderPort;
import com.proyecta.api_gestion.service.notification.NotificationTemplateRenderer;
import com.proyecta.api_gestion.service.notification.NotificationTemplateService;
import com.proyecta.api_gestion.service.security.dynamic.KeycloakIdentityExtractor;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final JavaMailSender javaMailSender;
    private final NotificationMailDispatchLogRepository mailDispatchLogRepository;
    private final NotificationAuditRepository auditRepository;
    private final NotificationTemplateRepository templateRepository;

    @Value("${mail.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:0}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public NotificationAdminController(NotificationEventCatalogRepository eventCatalogRepository,
                                       NotificationTemplateService templateService,
                                       NotificationPreferenceService preferenceService,
                                       NotificationTemplateRenderer renderer,
                                       NotificationSenderPort notificationSender,
                                       SeguridadUsuarioRepository usuarioRepository,
                                       KeycloakIdentityExtractor identityExtractor,
                                       JavaMailSender javaMailSender,
                                       NotificationMailDispatchLogRepository mailDispatchLogRepository,
                                       NotificationAuditRepository auditRepository,
                                       NotificationTemplateRepository templateRepository) {
        this.eventCatalogRepository = eventCatalogRepository;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
        this.notificationSender = notificationSender;
        this.usuarioRepository = usuarioRepository;
        this.identityExtractor = identityExtractor;
        this.javaMailSender = javaMailSender;
        this.mailDispatchLogRepository = mailDispatchLogRepository;
        this.auditRepository = auditRepository;
        this.templateRepository = templateRepository;
    }

    @GetMapping("/eventos")
    public ResponseEntity<ApiResponse<List<NotificationEventCatalogDTO>>> listEvents() {
        var data = eventCatalogRepository.findAll().stream().map(event ->
                new NotificationEventCatalogDTO(event.getCode(), event.getName(), event.getDescription(), event.getCategory(), event.getDefaultEnabled(), event.getActive(), event.getRequiresProjectContext())
        ).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Eventos de notificacion listados correctamente"));
    }

    @GetMapping("/plantillas")
    public ResponseEntity<ApiResponse<List<NotificationTemplateDTO>>> listTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search) {

        Map<String, String> eventCategoryMap = eventCatalogRepository.findAll().stream()
                .collect(Collectors.toMap(NotificationEventCatalog::getCode, NotificationEventCatalog::getCategory));

        List<NotificationTemplateDTO> data = templateService.listAll().stream()
                .filter(t -> category == null || category.isBlank() || category.equals(eventCategoryMap.get(t.getEventCode())))
                .filter(t -> severity == null || severity.isBlank() || severity.equals(t.getSeverity()))
                .filter(t -> enabled == null || t.getEnabled().equals(enabled))
                .filter(t -> search == null || search.isBlank()
                        || t.getEventCode().toLowerCase().contains(search.toLowerCase())
                        || eventCategoryMap.getOrDefault(t.getEventCode(), "").toLowerCase().contains(search.toLowerCase()))
                .map(template -> toDto(template, eventCategoryMap.get(template.getEventCode())))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(data, "Plantillas listadas correctamente"));
    }

    @GetMapping("/categorias")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCategories() {
        List<NotificationEventCatalog> events = eventCatalogRepository.findAll();

        Map<String, List<NotificationEventCatalog>> grouped = events.stream()
                .collect(Collectors.groupingBy(NotificationEventCatalog::getCategory));

        List<Map<String, Object>> result = grouped.entrySet().stream().map(entry -> {
            Map<String, Object> category = new LinkedHashMap<>();
            category.put("category", entry.getKey());
            category.put("eventCount", entry.getValue().size());
            category.put("events", entry.getValue().stream().map(e -> {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("code", e.getCode());
                event.put("name", e.getName());
                event.put("active", e.getActive());
                return event;
            }).toList());
            return category;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(result, "Categorias listadas correctamente"));
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
        String category = eventCatalogRepository.findById(saved.getEventCode())
                .map(NotificationEventCatalog::getCategory)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.success(toDto(saved, category), "Plantilla guardada correctamente"));
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSend(@Valid @RequestBody NotificationTestSendRequest request,
                                                                     Authentication authentication) {
        try {
            String username = identityExtractor.resolveUsername(authentication);
            var userOpt = usuarioRepository.findByUsernameIgnoreCase(username);
            if (userOpt.isEmpty() || userOpt.get().getCorreo() == null || userOpt.get().getCorreo().isBlank()) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("recipient", username);
                payload.put("status", "FAILED");
                payload.put("success", false);
                payload.put("errorMessage", "No se encontró correo electrónico para el usuario");
                return ResponseEntity.badRequest().body(ApiResponse.success(payload, "USER_NO_EMAIL"));
            }

            String recipientEmail = userOpt.get().getCorreo();
            var subject = renderer.apply(request.subjectTemplate(), request.variables());
            var body = renderer.apply(request.bodyTemplate(), request.variables());
            var message = new com.proyecta.api_gestion.service.notification.NotificationMessage(subject, body, Boolean.TRUE.equals(request.htmlEnabled()));

            var result = notificationSender.send(recipientEmail, message);
            log.info("Test notification email sent to {} (user: {})", recipientEmail, username);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recipient", recipientEmail);
            payload.put("status", result.status().name());
            payload.put("success", result.success());
            payload.put("errorMessage", result.errorMessage());
            return ResponseEntity.ok(ApiResponse.success(payload, "Correo de prueba procesado"));
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("status", "FAILED");
            payload.put("success", false);
            payload.put("errorMessage", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.success(payload, "EMAIL_SEND_FAILED"));
        }
    }

    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estadisticas() {
        Map<String, Object> result = new LinkedHashMap<>();

        long totalSent = auditRepository.countByChannelAndStatus("EMAIL", "SENT");
        long totalFailed = auditRepository.countByChannelAndStatus("EMAIL", "FAILED");
        long totalInApp = auditRepository.countByChannelAndStatus("IN_APP", "SENT");
        long totalInAppFailed = auditRepository.countByChannelAndStatus("IN_APP", "FAILED");

        long activeTemplates = templateRepository.countByEnabledTrue();
        long totalEvents = eventCatalogRepository.count();

        List<SeguridadUsuario> users = usuarioRepository.findAll();
        long usersWithEmail = users.stream()
                .filter(u -> u.getActivo() != null && u.getActivo())
                .filter(u -> u.getCorreo() != null && !u.getCorreo().isBlank())
                .count();
        long usersWithGlobalNotifs = users.stream()
                .filter(u -> u.getActivo() != null && u.getActivo())
                .filter(u -> Boolean.TRUE.equals(u.getRecibirNotificacionesGlobales()))
                .count();

        result.put("totalDispatched", totalSent + totalFailed + totalInApp + totalInAppFailed);
        result.put("totalSent", totalSent);
        result.put("totalFailed", totalFailed);
        result.put("inAppSent", totalInApp);
        result.put("inAppFailed", totalInAppFailed);
        result.put("activeTemplates", activeTemplates);
        result.put("totalEvents", totalEvents);
        result.put("totalUsers", users.size());
        result.put("usersWithEmail", usersWithEmail);
        result.put("usersWithGlobalNotifs", usersWithGlobalNotifs);

        return ResponseEntity.ok(ApiResponse.success(result, "Estadisticas de notificaciones"));
    }

    @GetMapping("/diagnostico")
    public ResponseEntity<ApiResponse<Map<String, Object>>> diagnostico() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("smtpEnabled", notificationsEnabled);
        result.put("smtpHost", smtpHost);
        result.put("smtpPort", smtpPort);
        result.put("smtpUsername", smtpUsername != null && !smtpUsername.isBlank() ? maskEmail(smtpUsername) : "(no configurado)");

        try {
            var mimeMessage = javaMailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setTo("test@test.com");
            helper.setSubject("Test");
            helper.setText("Test");
            javaMailSender.createMimeMessage();
            result.put("smtpConnection", "OK - JavaMailSender instanciado correctamente");
        } catch (Exception e) {
            result.put("smtpConnection", "ERROR - " + e.getMessage());
        }

        List<NotificationMailDispatchLog> recentLogs = mailDispatchLogRepository.findAll(
                PageRequest.of(
                        0,
                        10,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                )
        ).getContent();

        List<Map<String, Object>> logEntries = recentLogs.stream().map(logEntry -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("recipient", logEntry.getRecipient());
            entry.put("subject", logEntry.getSubject());
            entry.put("status", logEntry.getStatus());
            entry.put("detail", logEntry.getDetail());
            entry.put("createdAt", logEntry.getCreatedAt() != null ? logEntry.getCreatedAt().toString() : null);
            return entry;
        }).toList();
        result.put("recentDispatchLogs", logEntries);

        long totalSent = auditRepository.countByChannelAndStatus("EMAIL", "SENT");
        long totalFailed = auditRepository.countByChannelAndStatus("EMAIL", "FAILED");
        result.put("emailStats", Map.of("sent", totalSent, "failed", totalFailed));

        List<NotificationTemplate> templates = templateRepository.findAll();
        List<Map<String, Object>> templateStatus = templates.stream().map(t -> {
            Map<String, Object> ts = new LinkedHashMap<>();
            ts.put("eventCode", t.getEventCode());
            ts.put("enabled", t.getEnabled());
            ts.put("htmlEnabled", t.getHtmlEnabled());
            return ts;
        }).toList();
        result.put("templates", templateStatus);

        List<SeguridadUsuario> users = usuarioRepository.findAll();
        List<Map<String, Object>> userInfo = users.stream().map(u -> {
            Map<String, Object> ui = new LinkedHashMap<>();
            ui.put("username", u.getUsername());
            ui.put("correo", u.getCorreo() != null ? maskEmail(u.getCorreo()) : "(null)");
            ui.put("globalNotifications", u.getRecibirNotificacionesGlobales());
            ui.put("activo", u.getActivo());
            return ui;
        }).toList();
        result.put("users", userInfo);

        return ResponseEntity.ok(ApiResponse.success(result, "Diagnostico del sistema de correo"));
    }

    @GetMapping("/fallidas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFailedNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime fromDate = parseDateTime(from);
        LocalDateTime toDate = parseDateTime(to);
        String status = "FAILED";

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationAudit> auditPage = auditRepository.findFailedWithFilters(
                status, channel, eventCode, recipient, fromDate, toDate, pageable);

        List<NotificationAuditDTO> entries = auditPage.getContent().stream()
                .map(a -> new NotificationAuditDTO(
                        a.getId(), a.getEventCode(), a.getRecipient(),
                        a.getChannel(), a.getStatus(), a.getFailureReason(), a.getCreatedAt()))
                .toList();

        long totalFailedEmail = auditRepository.countByChannelAndStatus("EMAIL", "FAILED");
        long totalFailedInApp = auditRepository.countByChannelAndStatus("IN_APP", "FAILED");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries);
        result.put("totalElements", auditPage.getTotalElements());
        result.put("totalPages", auditPage.getTotalPages());
        result.put("currentPage", auditPage.getNumber());
        result.put("totalFailedEmail", totalFailedEmail);
        result.put("totalFailedInApp", totalFailedInApp);

        return ResponseEntity.ok(ApiResponse.success(result, "Notificaciones fallidas listadas correctamente"));
    }

    @GetMapping("/fallidas/detalle-dispatch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFailedDispatchLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Instant fromDate = parseInstant(from);
        Instant toDate = parseInstant(to);
        String status = "FAILED";

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationMailDispatchLog> logPage = mailDispatchLogRepository.findFailedWithFilters(
                status, recipient, fromDate, toDate, pageable);

        List<NotificationDispatchLogDTO> entries = logPage.getContent().stream()
                .map(l -> new NotificationDispatchLogDTO(
                        l.getId(), l.getRecipient(), l.getSubject(),
                        l.getStatus(), l.getDetail(), l.getCreatedAt()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries);
        result.put("totalElements", logPage.getTotalElements());
        result.put("totalPages", logPage.getTotalPages());
        result.put("currentPage", logPage.getNumber());

        return ResponseEntity.ok(ApiResponse.success(result, "Detalle de dispatch fallidos listado correctamente"));
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) return "**" + domain;
        return local.substring(0, 2) + "**" + domain;
    }

    private NotificationTemplateDTO toDto(NotificationTemplate template, String category) {
        return new NotificationTemplateDTO(
                template.getEventCode(),
                template.getEnabled(),
                template.getHtmlEnabled(),
                template.getSeverity(),
                template.getScope(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.getTargetRoles(),
                template.getUpdatedBy(),
                category
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
