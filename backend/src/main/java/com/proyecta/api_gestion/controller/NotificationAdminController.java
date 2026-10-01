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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Tag(name = "Administración - Notificaciones", description = "Endpoints de administración de plantillas, reglas, agrupación y envío de notificaciones")
@PreAuthorize("@proyectoSecurity.canAccessGlobal('SISTEMA:CONFIGURAR', authentication)")
public class NotificationAdminController {
    private static final Logger log = LoggerFactory.getLogger(NotificationAdminController.class);

    private static final String KEY_RECIPIENT = "recipient";
    private static final String KEY_STATUS = "status";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_ERROR_MESSAGE = "errorMessage";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_SENT = "SENT";
    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_IN_APP = "IN_APP";

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

    @Operation(summary = "Listar el catálogo de eventos de notificación")
    @GetMapping("/eventos")
    public ResponseEntity<ApiResponse<List<NotificationEventCatalogDTO>>> listEvents() {
        var data = eventCatalogRepository.findAll().stream().map(event ->
                new NotificationEventCatalogDTO(event.getCode(), event.getName(), event.getDescription(), event.getCategory(), event.getDefaultEnabled(), event.getActive(), event.getRequiresProjectContext())
        ).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Eventos de notificacion listados correctamente"));
    }

    @Operation(summary = "Listar las plantillas de notificación configuradas")
    @GetMapping("/plantillas")
    public ResponseEntity<ApiResponse<List<NotificationTemplateDTO>>> listTemplates() {

        Map<String, NotificationEventCatalog> eventMap = eventCatalogRepository.findAll().stream()
                .collect(Collectors.toMap(NotificationEventCatalog::getCode, e -> e));

        List<NotificationTemplateDTO> data = templateService.listAll().stream()
                .map(template -> {
                    var event = eventMap.get(template.getEventCode());
                    return toDto(template, event);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(data, "Plantillas listadas correctamente"));
    }

    @Operation(
        summary = "Crear o actualizar una plantilla de notificación",
        description = "Realiza un upsert de la plantilla asociada al código de evento indicado (habilitación, severidad, alcance, asunto, cuerpo y roles destino)."
    )
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
        var event = eventCatalogRepository.findById(saved.getEventCode()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(toDto(saved, event), "Plantilla guardada correctamente"));
    }

    @Operation(summary = "Listar las preferencias de notificación de los usuarios")
    @GetMapping("/preferencias")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDTO>>> listPreferences() {
        var data = preferenceService.listAll().stream().map(this::toPreferenceDto).toList();
        return ResponseEntity.ok(ApiResponse.success(data, "Preferencias listadas correctamente"));
    }

    @Operation(
        summary = "Crear o actualizar una preferencia de notificación",
        description = "Guarda la preferencia de un usuario para un evento, con alcance global o por proyecto y activación de correo."
    )
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

    @Operation(
        summary = "Previsualizar una plantilla de notificación",
        description = "Renderiza el asunto y el cuerpo de la plantilla con las variables proporcionadas, sin enviar ninguna notificación."
    )
    @PostMapping("/plantillas/preview")
    public ResponseEntity<ApiResponse<NotificationTemplatePreviewResponse>> preview(@RequestBody NotificationTemplatePreviewRequest request) {
        var subject = renderer.apply(request.subjectTemplate(), request.variables());
        var body = renderer.apply(request.bodyTemplate(), request.variables());
        return ResponseEntity.ok(ApiResponse.success(new NotificationTemplatePreviewResponse(subject, body, request.htmlEnabled()), "Preview generado correctamente"));
    }

    @Operation(
        summary = "Enviar un correo de prueba de notificación",
        description = "Renderiza la plantilla y envía un correo de prueba al usuario autenticado. Retorna el estado del envío (incluido el error si el usuario no tiene correo configurado)."
    )
    @PostMapping("/plantillas/test-send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testSend(@Valid @RequestBody NotificationTestSendRequest request,
                                                                     Authentication authentication) {
        try {
            String username = identityExtractor.resolveUsername(authentication);
            var userOpt = usuarioRepository.findByUsernameIgnoreCase(username);
            if (userOpt.isEmpty() || userOpt.get().getCorreo() == null || userOpt.get().getCorreo().isBlank()) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put(KEY_RECIPIENT, username);
                payload.put(KEY_STATUS, STATUS_FAILED);
                payload.put(KEY_SUCCESS, false);
                payload.put(KEY_ERROR_MESSAGE, "No se encontró correo electrónico para el usuario");
                return ResponseEntity.badRequest().body(ApiResponse.success(payload, "USER_NO_EMAIL"));
            }

            String recipientEmail = userOpt.get().getCorreo();
            var subject = renderer.apply(request.subjectTemplate(), request.variables());
            var body = renderer.apply(request.bodyTemplate(), request.variables());
            var message = new com.proyecta.api_gestion.service.notification.NotificationMessage(subject, body, Boolean.TRUE.equals(request.htmlEnabled()));

            var result = notificationSender.send(recipientEmail, message);
            String safeRecipientEmail = com.proyecta.api_gestion.infrastructure.LogSanitizer.clean(recipientEmail);
            String safeUsername = com.proyecta.api_gestion.infrastructure.LogSanitizer.clean(username);
            log.info("Test notification email sent to {} (user: {})", safeRecipientEmail, safeUsername);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put(KEY_RECIPIENT, recipientEmail);
            payload.put(KEY_STATUS, result.status().name());
            payload.put(KEY_SUCCESS, result.success());
            payload.put(KEY_ERROR_MESSAGE, result.errorMessage());
            return ResponseEntity.ok(ApiResponse.success(payload, "Correo de prueba procesado"));
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage(), e);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put(KEY_STATUS, STATUS_FAILED);
            payload.put(KEY_SUCCESS, false);
            payload.put(KEY_ERROR_MESSAGE, e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.success(payload, "EMAIL_SEND_FAILED"));
        }
    }

    @Operation(summary = "Obtener estadísticas del sistema de notificaciones")
    @GetMapping("/estadisticas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> estadisticas() {
        Map<String, Object> result = new LinkedHashMap<>();

        long totalSent = auditRepository.countByChannelAndStatus(CHANNEL_EMAIL, STATUS_SENT);
        long totalFailed = auditRepository.countByChannelAndStatus(CHANNEL_EMAIL, STATUS_FAILED);
        long totalInApp = auditRepository.countByChannelAndStatus(CHANNEL_IN_APP, STATUS_SENT);
        long totalInAppFailed = auditRepository.countByChannelAndStatus(CHANNEL_IN_APP, STATUS_FAILED);

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

    @Operation(
        summary = "Obtener diagnóstico de la configuración de correo",
        description = "Expone el estado de la configuración SMTP (con datos sensibles enmascarados), las últimas trazas de envío, plantillas y usuarios con notificaciones activas."
    )
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
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, KEY_CREATED_AT)
                )
        ).getContent();

        List<Map<String, Object>> logEntries = recentLogs.stream().map(logEntry -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put(KEY_RECIPIENT, logEntry.getRecipient());
            entry.put("subject", logEntry.getSubject());
            entry.put(KEY_STATUS, logEntry.getStatus());
            entry.put("detail", logEntry.getDetail());
            entry.put(KEY_CREATED_AT, logEntry.getCreatedAt() != null ? logEntry.getCreatedAt().toString() : null);
            return entry;
        }).toList();
        result.put("recentDispatchLogs", logEntries);

        long totalSent = auditRepository.countByChannelAndStatus(CHANNEL_EMAIL, STATUS_SENT);
        long totalFailed = auditRepository.countByChannelAndStatus(CHANNEL_EMAIL, STATUS_FAILED);
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

    @Operation(
        summary = "Listar notificaciones fallidas",
        description = "Retorna de forma paginada las notificaciones con estado FAILED, filtrables por canal, código de evento, destinatario y rango de fechas (ISO-8601)."
    )
    @GetMapping("/fallidas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFailedNotifications(
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtra por canal (EMAIL, IN_APP)") @RequestParam(required = false) String channel,
            @Parameter(description = "Filtra por código del evento") @RequestParam(required = false) String eventCode,
            @Parameter(description = "Filtra por destinatario") @RequestParam(required = false) String recipient,
            @Parameter(description = "Fecha inicio del rango (ISO-8601)") @RequestParam(required = false) String from,
            @Parameter(description = "Fecha fin del rango (ISO-8601)") @RequestParam(required = false) String to) {

        LocalDateTime fromDate = parseDateTime(from);
        LocalDateTime toDate = parseDateTime(to);
        String status = STATUS_FAILED;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, KEY_CREATED_AT));
        Page<NotificationAudit> auditPage = auditRepository.findFailedWithFilters(
                status, channel, eventCode, recipient, fromDate, toDate, pageable);

        List<NotificationAuditDTO> entries = auditPage.getContent().stream()
                .map(a -> new NotificationAuditDTO(
                        a.getId(), a.getEventCode(), a.getRecipient(),
                        a.getChannel(), a.getStatus(), a.getFailureReason(), a.getCreatedAt()))
                .toList();

        long totalFailedEmail = auditRepository.countByChannelAndStatus(CHANNEL_EMAIL, STATUS_FAILED);
        long totalFailedInApp = auditRepository.countByChannelAndStatus(CHANNEL_IN_APP, STATUS_FAILED);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries);
        result.put("totalElements", auditPage.getTotalElements());
        result.put("totalPages", auditPage.getTotalPages());
        result.put("currentPage", auditPage.getNumber());
        result.put("totalFailedEmail", totalFailedEmail);
        result.put("totalFailedInApp", totalFailedInApp);

        return ResponseEntity.ok(ApiResponse.success(result, "Notificaciones fallidas listadas correctamente"));
    }

    @Operation(
        summary = "Listar trazas de despacho fallidas de correo",
        description = "Retorna de forma paginada el detalle de los envíos de correo fallidos, filtrable por destinatario y rango de fechas (ISO-8601)."
    )
    @GetMapping("/fallidas/detalle-dispatch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listFailedDispatchLogs(
            @Parameter(description = "Número de página (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filtra por destinatario") @RequestParam(required = false) String recipient,
            @Parameter(description = "Fecha inicio del rango (ISO-8601)") @RequestParam(required = false) String from,
            @Parameter(description = "Fecha fin del rango (ISO-8601)") @RequestParam(required = false) String to) {

        Instant fromDate = parseInstant(from);
        Instant toDate = parseInstant(to);
        String status = STATUS_FAILED;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, KEY_CREATED_AT));
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
        } catch (DateTimeParseException _) {
            return null;
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException _) {
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

    private NotificationTemplateDTO toDto(NotificationTemplate template, NotificationEventCatalog event) {
        return new NotificationTemplateDTO(
                template.getEventCode(),
                event != null ? event.getName() : template.getEventCode(),
                template.getEnabled(),
                template.getHtmlEnabled(),
                template.getSeverity(),
                template.getScope(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.getTargetRoles(),
                template.getUpdatedBy(),
                event != null ? event.getCategory() : null
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
