package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.config.FrontendUrlProperties;
import com.proyecta.api_gestion.domain.model.notification.NotificationAudit;
import com.proyecta.api_gestion.domain.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationAuditRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestratorService.class);

    private static final String ROUTE_CLOSURE = "/closure";
    private static final String ROUTE_PROGRESS = "/progress";
    private static final String ROUTE_RISKS = "/risks";
    private static final String CHANNEL_EMAIL = "EMAIL";

    private static final Map<String, String> EVENT_ROUTE_MAP = Map.ofEntries(
            Map.entry("CLOSURE_REQUESTED", ROUTE_CLOSURE),
            Map.entry("CLOSURE_APPROVED", ROUTE_CLOSURE),
            Map.entry("CLOSURE_REJECTED", ROUTE_CLOSURE),
            Map.entry("PROJECT_CLOSED", ROUTE_CLOSURE),
            Map.entry("DELIVERABLE_EVIDENCE_UPLOADED", ROUTE_PROGRESS),
            Map.entry("DELIVERABLE_APPROVED", ROUTE_PROGRESS),
            Map.entry("DELIVERABLE_REJECTED", ROUTE_PROGRESS),
            Map.entry("OBSERVATION_SUBSANATED", ROUTE_PROGRESS),
            Map.entry("PROJECT_DELAYED", "/schedule"),
            Map.entry("PROJECT_INITIAL_REGISTERED", ROUTE_PROGRESS),
            Map.entry("PROJECT_INITIAL_COMPLETED", ROUTE_PROGRESS),
            Map.entry("PROJECT_UPDATED", ROUTE_PROGRESS),
            Map.entry("PROJECT_DOCUMENT_UPLOADED", ROUTE_PROGRESS),
            Map.entry("PROJECT_DOCUMENT_DELETED", ROUTE_PROGRESS),
            Map.entry("PROJECT_BENEFIT_IMPACT_REQUIRED", ROUTE_PROGRESS),
            Map.entry("PROJECT_BENEFIT_IMPACT_SUBMITTED", ROUTE_PROGRESS),
            Map.entry("PROJECT_BENEFIT_IMPACT_RESUBMITTED", ROUTE_PROGRESS),
            Map.entry("PROJECT_BENEFIT_IMPACT_REVIEWED", ROUTE_PROGRESS),
            Map.entry("RISK_CREATED", ROUTE_RISKS),
            Map.entry("RISK_UPDATED", ROUTE_RISKS),
            Map.entry("RISK_TREATED", ROUTE_RISKS),
            Map.entry("RISK_DELETED", ROUTE_RISKS),
            Map.entry("EVIDENCE_VERSION_REVERTED", "/evidences"),
            Map.entry("ADVANCE_REPORT_UPLOADED", ROUTE_PROGRESS),
            Map.entry("ADVANCE_REPORT_VERIFIED", ROUTE_PROGRESS),
            Map.entry("ADVANCE_REPORT_RETURNED", ROUTE_PROGRESS),
            Map.entry("ENTREGABLE_FECHA_CAMBIADA", "/schedule"),
            Map.entry("ENTREGABLE_DEADLINE_WARNING", ROUTE_PROGRESS),
            Map.entry("ENTREGABLE_OVERDUE_REMINDER", ROUTE_PROGRESS),
            Map.entry("PROJECT_DIRECTOR_ALERT", ROUTE_PROGRESS),
            Map.entry("SECURITY_ROLE_UPDATED", "/admin/configuracion"),
            Map.entry("SECURITY_USER_UPDATED", "/admin/configuracion"),
            Map.entry("PROJECT_ASSIGNMENT_CREATED", ROUTE_PROGRESS),
            Map.entry("VIABILIDAD_UPLOADED", ROUTE_PROGRESS),
            Map.entry("VIABILIDAD_APPROVED", ROUTE_PROGRESS),
            Map.entry("VIABILIDAD_RETURNED", ROUTE_PROGRESS),
            Map.entry("ACTA_CONSTITUCION_REMINDER", ROUTE_PROGRESS),
            Map.entry("ADVANCE_REPORT_DUE_NOTIFICATION", ROUTE_PROGRESS)
    );

    private final NotificationRecipientResolverPort recipientResolver;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateRendererPort renderer;
    private final NotificationSenderPort sender;
    private final InAppNotificationService inAppService;
    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;
    private final NotificationAuditRepositoryPort auditRepositoryPort;
    private final NotificationMailDispatchTracker mailDispatchTracker;
    private final NotificationActorResolver actorResolver;
    private final FrontendUrlProperties frontendUrlProperties;
    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final SystemParameterService systemParameterService;

    public NotificationOrchestratorService(NotificationRecipientResolverPort recipientResolver,
                                           NotificationTemplateService templateService,
                                           NotificationPreferenceService preferenceService,
                                           NotificationTemplateRendererPort renderer,
                                           NotificationSenderPort sender,
                                           InAppNotificationService inAppService,
                                           SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
                                           NotificationAuditRepositoryPort auditRepositoryPort,
                                           NotificationMailDispatchTracker mailDispatchTracker,
                                           NotificationActorResolver actorResolver,
                                           FrontendUrlProperties frontendUrlProperties,
                                           ProyectoRepositoryPort proyectoRepositoryPort,
                                           SystemParameterService systemParameterService) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
        this.sender = sender;
        this.inAppService = inAppService;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.auditRepositoryPort = auditRepositoryPort;
        this.mailDispatchTracker = mailDispatchTracker;
        this.actorResolver = actorResolver;
        this.frontendUrlProperties = frontendUrlProperties;
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.systemParameterService = systemParameterService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationContext context) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            NotificationTemplate template = templateService.get(context.eventType().name());
            if (!Boolean.TRUE.equals(template.getEnabled())) {
                log.debug("[Notification] Template disabled for event {}", context.eventType());
                return;
            }

            PreparedModel prepared = prepareModel(context);
            Map<String, Object> model = prepared.model();
            String absoluteTargetUrl = prepared.absoluteTargetUrl();

            var message = renderer.render(template, model);
            String title = String.valueOf(model.getOrDefault("title", message.subject()));
            Set<String> actorIdentifiers = actorResolver.resolveActorIdentifiers(context.actorUsername());

            List<String> contextualRecipients = recipientResolver.resolveRecipients(context);
            Set<String> finalRecipients = new HashSet<>();
            addContextualRecipients(finalRecipients, contextualRecipients, actorIdentifiers);

            List<SeguridadUsuario> globalRecipients = usuarioRepositoryPort.findByRecibirNotificacionesGlobalesTrue();
            addGlobalRecipients(finalRecipients, globalRecipients, actorIdentifiers);

            log.debug("[Notification] Dispatching event {} to {} final recipients", context.eventType(), finalRecipients.size());

            String relativeTargetUrl = resolveTargetUrl(context.eventType().name(), context.projectId());

            for (String recipient : finalRecipients) {
                createInAppNotification(recipient, title, message, context, template,
                        absoluteTargetUrl, relativeTargetUrl);
                sendEmailNotification(recipient, message, context, model);
            }
        } finally {
            MDC.clear();
        }
    }

    private record PreparedModel(Map<String, Object> model, String absoluteTargetUrl) {
    }

    private PreparedModel prepareModel(NotificationContext context) {
        Map<String, Object> model = context.attributes() == null ? new HashMap<>() : new HashMap<>(context.attributes());
        model.putIfAbsent("eventCode", context.eventType().name());
        model.putIfAbsent("projectId", context.projectId());

        if (!model.containsKey("state") && context.projectId() != null && !context.projectId().isBlank()) {
            proyectoRepositoryPort.findById(context.projectId())
                    .ifPresent(p -> {
                        String stateCode = p.getEstadoCodigo();
                        model.putIfAbsent("state", stateCode != null ? stateCode : String.valueOf(p.getEstado()));
                    });
        }

        String resolvedActorName = resolveActorDisplayName(context.actorUsername());
        model.putIfAbsent("actorUsername", resolvedActorName);

        resolveActorNameInModel(model, "requester");
        resolveActorNameInModel(model, "approver");
        resolveActorNameInModel(model, "rejector");
        resolveActorNameInModel(model, "sentBy");
        resolveActorNameInModel(model, "assignedUsername");

        String targetUrl = resolveTargetUrl(context.eventType().name(), context.projectId());
        String absoluteTargetUrl = buildAbsoluteUrl(targetUrl);
        model.putIfAbsent("targetUrl", absoluteTargetUrl != null ? absoluteTargetUrl : "");

        applyPreWizardConfigurableMessages(context.eventType().name(), model);
        return new PreparedModel(model, absoluteTargetUrl);
    }

    private void addContextualRecipients(Set<String> finalRecipients, List<String> contextualRecipients,
                                         Set<String> actorIdentifiers) {
        for (String recipient : contextualRecipients) {
            if (recipient == null || recipient.isBlank()) {
                continue;
            }
            if (actorResolver.isActor(recipient, actorIdentifiers)) {
                log.info("[Notification] Omitido: {} (Regla 2 - Actor Original)", recipient);
            } else {
                finalRecipients.add(recipient);
            }
        }
    }

    private void addGlobalRecipients(Set<String> finalRecipients, List<SeguridadUsuario> globalRecipients,
                                     Set<String> actorIdentifiers) {
        for (SeguridadUsuario user : globalRecipients) {
            if (user == null) {
                continue;
            }
            if (actorResolver.isActor(user.getUsername(), actorIdentifiers)
                    || actorResolver.isActor(user.getCorreo(), actorIdentifiers)
                    || actorResolver.isActor(user.getNombre(), actorIdentifiers)) {
                log.info("[Notification] Omitido: {} (Regla 2 - Actor Original)", user.getUsername());
            } else {
                finalRecipients.add(user.getUsername());
                log.info("[Notification] Añadido Admin: {} (Regla 3 - Flag Global)", user.getUsername());
            }
        }
    }

    private void createInAppNotification(String recipient, String title, NotificationMessage message,
                                         NotificationContext context, NotificationTemplate template,
                                         String absoluteTargetUrl, String relativeTargetUrl) {
        try {
            inAppService.create(recipient, title, toInAppPlainText(message.body(), absoluteTargetUrl),
                    context.eventType().name(), template.getSeverity(), context.projectId(), relativeTargetUrl);
            auditRepositoryPort.save(new NotificationAudit(context.eventType().name(), recipient, "IN_APP", "SENT", null));
        } catch (Exception ex) {
            log.warn("[Notification] In-app creation failed for recipient {} - {}", recipient, ex.getMessage());
            auditRepositoryPort.save(new NotificationAudit(context.eventType().name(), recipient, "IN_APP", "FAILED", ex.getMessage()));
        }
    }

    private void sendEmailNotification(String recipient, NotificationMessage message,
                                       NotificationContext context, Map<String, Object> model) {
        try {
            SeguridadUsuario resolvedRecipient = usuarioRepositoryPort.findByUsernameIgnoreCase(recipient)
                    .or(() -> usuarioRepositoryPort.findByCorreoIgnoreCase(recipient))
                    .orElse(null);
            String emailAddress = resolveEmailAddress(resolvedRecipient, recipient);

            boolean globalNotificationsEnabled = resolvedRecipient != null
                    && Boolean.TRUE.equals(resolvedRecipient.getRecibirNotificacionesGlobales());
            boolean emailAllowed = globalNotificationsEnabled
                    || preferenceService.isEnabled(recipient, context.eventType().name(), context.projectId());

            if (!emailAllowed) {
                log.info("[Notification] Email omitido para {}: Preferencias apagadas", recipient);
                mailDispatchTracker.skipped(recipient, message.subject(), "Preferencias de correo desactivadas");
                return;
            }

            NotificationMessage threadedMessage = buildThreadedMessage(message, context, model);
            var result = sender.send(emailAddress, threadedMessage);
            recordEmailResult(recipient, emailAddress, context, result);
        } catch (Exception ex) {
            log.warn("[Notification] Email send failed for recipient {} - {}", recipient, ex.getMessage());
            mailDispatchTracker.failed(recipient, message.subject(), ex.getMessage());
            auditRepositoryPort.save(new NotificationAudit(context.eventType().name(), recipient, CHANNEL_EMAIL, "FAILED", ex.getMessage()));
        }
    }

    private NotificationMessage buildThreadedMessage(NotificationMessage message, NotificationContext context,
                                                     Map<String, Object> model) {
        if (context.projectId() != null && !context.projectId().isBlank()) {
            String projectName = String.valueOf(model.getOrDefault("projectName", context.projectId()));
            return message.withThread(context.projectId(), projectName);
        }
        return message;
    }

    private void recordEmailResult(String recipient, String emailAddress, NotificationContext context,
                                   NotificationSendResult result) {
        if (result.success()) {
            log.debug("[Notification] Email sent to {}", emailAddress);
            auditRepositoryPort.save(new NotificationAudit(context.eventType().name(), recipient, CHANNEL_EMAIL, result.status().name(), null));
        } else {
            auditRepositoryPort.save(new NotificationAudit(context.eventType().name(), recipient, CHANNEL_EMAIL, result.status().name(), result.errorMessage()));
        }
    }

    private void applyPreWizardConfigurableMessages(String eventCode, Map<String, Object> model) {
        String tituloKey;
        String introKey;
        switch (eventCode) {
            case "VIABILIDAD_UPLOADED" -> {
                tituloKey = SystemParameterKeys.NOTIF_PREWIZARD_CARGUE_TITULO;
                introKey = SystemParameterKeys.NOTIF_PREWIZARD_CARGUE_INTRO;
            }
            case "VIABILIDAD_APPROVED" -> {
                tituloKey = SystemParameterKeys.NOTIF_PREWIZARD_APROBADO_TITULO;
                introKey = SystemParameterKeys.NOTIF_PREWIZARD_APROBADO_INTRO;
            }
            case "VIABILIDAD_RETURNED" -> {
                tituloKey = SystemParameterKeys.NOTIF_PREWIZARD_DEVUELTO_TITULO;
                introKey = SystemParameterKeys.NOTIF_PREWIZARD_DEVUELTO_INTRO;
            }
            default -> {
                return;
            }
        }

        model.putIfAbsent("mensajeTitulo", systemParameterService.getString(tituloKey, ""));
        model.putIfAbsent("mensajeIntro", systemParameterService.getString(introKey, ""));
        model.putIfAbsent("estadoEtiqueta", systemParameterService.getString(
                SystemParameterKeys.NOTIF_PREWIZARD_ESTADO_ETIQUETA,
                "Estado de los documentos iniciales del proyecto:"));
        model.putIfAbsent("enlaceTexto", systemParameterService.getString(
                SystemParameterKeys.NOTIF_PREWIZARD_ENLACE_TEXTO,
                "Puede acceder directamente a través del siguiente enlace:"));
    }

    private String resolveTargetUrl(String eventCode, String projectId) {
        String route = EVENT_ROUTE_MAP.get(eventCode);
        if (route == null) {
            return null;
        }
        if (projectId == null || projectId.isBlank()) {
            return route;
        }
        return "/projects/" + projectId.toLowerCase() + route;
    }

    private String resolveEmailAddress(SeguridadUsuario user, String fallbackRecipient) {
        if (user != null) {
            if (user.getCorreo() != null && !user.getCorreo().isBlank()) {
                return user.getCorreo().trim();
            }
            if (user.getUsername() != null && !user.getUsername().isBlank()) {
                return user.getUsername().trim();
            }
        }
        return fallbackRecipient;
    }

    private String resolveActorDisplayName(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }
        return usuarioRepositoryPort.findByUsernameIgnoreCase(username)
                .map(SeguridadUsuario::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .orElse(username);
    }

    private void resolveActorNameInModel(Map<String, Object> model, String attributeKey) {
        Object value = model.get(attributeKey);
        if (value instanceof String strValue && !strValue.isBlank()) {
            model.put(attributeKey, resolveActorDisplayName(strValue));
        }
    }

    private String buildAbsoluteUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String base = frontendUrlProperties.getBase();
        if (base == null || base.isBlank()) {
            return relativePath;
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) + relativePath
                : base + relativePath;
    }

    private String toInAppPlainText(String htmlBody, String absoluteTargetUrl) {
        if (htmlBody == null || htmlBody.isBlank()) {
            return "";
        }
        String text = htmlBody
                .replaceAll("(?is)<style[^>]*>.*?</style>", " ")
                .replaceAll("(?is)<script[^>]*>.*?</script>", " ")
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</div>", "\n")
                .replaceAll("(?i)</li>", " · ")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        if (absoluteTargetUrl != null && !absoluteTargetUrl.isBlank()) {
            text = text.replace(absoluteTargetUrl, " ");
            String relativePath = absoluteTargetUrl.replaceFirst("^https?://[^/]+", "");
            if (!relativePath.isBlank() && !"/".equals(relativePath)) {
                text = text.replace(relativePath, " ");
            }
        }
        text = text.replaceAll("https?://\\S+", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 600 ? text.substring(0, 597).trim() + "..." : text;
    }
}
