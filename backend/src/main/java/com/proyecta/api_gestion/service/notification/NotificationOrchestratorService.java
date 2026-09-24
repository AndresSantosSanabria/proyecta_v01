package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.config.FrontendUrlProperties;
import com.proyecta.api_gestion.model.notification.NotificationAudit;
import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.notification.NotificationAuditRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
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

    private static final Map<String, String> EVENT_ROUTE_MAP = Map.ofEntries(
            Map.entry("CLOSURE_REQUESTED", "/closure"),
            Map.entry("CLOSURE_APPROVED", "/closure"),
            Map.entry("CLOSURE_REJECTED", "/closure"),
            Map.entry("PROJECT_CLOSED", "/closure"),
            Map.entry("DELIVERABLE_EVIDENCE_UPLOADED", "/progress"),
            Map.entry("DELIVERABLE_APPROVED", "/progress"),
            Map.entry("DELIVERABLE_REJECTED", "/progress"),
            Map.entry("OBSERVATION_SUBSANATED", "/progress"),
            Map.entry("PROJECT_DELAYED", "/schedule"),
            Map.entry("PROJECT_INITIAL_REGISTERED", "/progress"),
            Map.entry("PROJECT_INITIAL_COMPLETED", "/progress"),
            Map.entry("PROJECT_UPDATED", "/progress"),
            Map.entry("PROJECT_DOCUMENT_UPLOADED", "/progress"),
            Map.entry("PROJECT_DOCUMENT_DELETED", "/progress"),
            Map.entry("PROJECT_BENEFIT_IMPACT_REQUIRED", "/progress"),
            Map.entry("PROJECT_BENEFIT_IMPACT_SUBMITTED", "/progress"),
            Map.entry("PROJECT_BENEFIT_IMPACT_RESUBMITTED", "/progress"),
            Map.entry("PROJECT_BENEFIT_IMPACT_REVIEWED", "/progress"),
            Map.entry("RISK_CREATED", "/risks"),
            Map.entry("RISK_UPDATED", "/risks"),
            Map.entry("RISK_TREATED", "/risks"),
            Map.entry("RISK_DELETED", "/risks"),
            Map.entry("EVIDENCE_VERSION_REVERTED", "/evidences"),
            Map.entry("ADVANCE_REPORT_UPLOADED", "/progress"),
            Map.entry("ADVANCE_REPORT_VERIFIED", "/progress"),
            Map.entry("ADVANCE_REPORT_RETURNED", "/progress"),
            Map.entry("ENTREGABLE_FECHA_CAMBIADA", "/schedule"),
            Map.entry("ENTREGABLE_DEADLINE_WARNING", "/progress"),
            Map.entry("ENTREGABLE_OVERDUE_REMINDER", "/progress"),
            Map.entry("PROJECT_DIRECTOR_ALERT", "/progress"),
            Map.entry("SECURITY_ROLE_UPDATED", "/admin/configuracion"),
            Map.entry("SECURITY_USER_UPDATED", "/admin/configuracion"),
            Map.entry("PROJECT_ASSIGNMENT_CREATED", "/progress"),
            Map.entry("VIABILIDAD_UPLOADED", "/progress"),
            Map.entry("VIABILIDAD_APPROVED", "/progress"),
            Map.entry("VIABILIDAD_RETURNED", "/progress"),
            Map.entry("ACTA_CONSTITUCION_REMINDER", "/progress"),
            Map.entry("ADVANCE_REPORT_DUE_NOTIFICATION", "/progress")
    );

    private final NotificationRecipientResolverPort recipientResolver;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateRendererPort renderer;
    private final NotificationSenderPort sender;
    private final InAppNotificationService inAppService;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final NotificationAuditRepository auditRepository;
    private final NotificationMailDispatchTracker mailDispatchTracker;
    private final NotificationActorResolver actorResolver;
    private final FrontendUrlProperties frontendUrlProperties;
    private final ProyectoRepository proyectoRepository;
    private final SystemParameterService systemParameterService;

    public NotificationOrchestratorService(NotificationRecipientResolverPort recipientResolver,
                                           NotificationTemplateService templateService,
                                           NotificationPreferenceService preferenceService,
                                           NotificationTemplateRendererPort renderer,
                                           NotificationSenderPort sender,
                                           InAppNotificationService inAppService,
                                           SeguridadUsuarioRepository usuarioRepository,
                                           NotificationAuditRepository auditRepository,
                                           NotificationMailDispatchTracker mailDispatchTracker,
                                           NotificationActorResolver actorResolver,
                                           FrontendUrlProperties frontendUrlProperties,
                                           ProyectoRepository proyectoRepository,
                                           SystemParameterService systemParameterService) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
        this.sender = sender;
        this.inAppService = inAppService;
        this.usuarioRepository = usuarioRepository;
        this.auditRepository = auditRepository;
        this.mailDispatchTracker = mailDispatchTracker;
        this.actorResolver = actorResolver;
        this.frontendUrlProperties = frontendUrlProperties;
        this.proyectoRepository = proyectoRepository;
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

            Map<String, Object> model = context.attributes() == null ? new HashMap<>() : new HashMap<>(context.attributes());
            model.putIfAbsent("eventCode", context.eventType().name());
            model.putIfAbsent("projectId", context.projectId());

            if (!model.containsKey("state") && context.projectId() != null && !context.projectId().isBlank()) {
                proyectoRepository.findById(context.projectId())
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

            var message = renderer.render(template, model);
            String title = String.valueOf(model.getOrDefault("title", message.subject()));
            Set<String> actorIdentifiers = actorResolver.resolveActorIdentifiers(context.actorUsername());

            List<String> contextualRecipients = recipientResolver.resolveRecipients(context);
            Set<String> finalRecipients = new HashSet<>();

            for (String recipient : contextualRecipients) {
                if (recipient == null || recipient.isBlank()) {
                    continue;
                }
                if (actorResolver.isActor(recipient, actorIdentifiers)) {
                    log.info("[Notification] Omitido: {} (Regla 2 - Actor Original)", recipient);
                    continue;
                }
                finalRecipients.add(recipient);
            }

            List<SeguridadUsuario> globalRecipients = usuarioRepository.findByRecibirNotificacionesGlobalesTrue();
            for (SeguridadUsuario user : globalRecipients) {
                if (user == null) {
                    continue;
                }
                if (actorResolver.isActor(user.getUsername(), actorIdentifiers)
                        || actorResolver.isActor(user.getCorreo(), actorIdentifiers)
                        || actorResolver.isActor(user.getNombre(), actorIdentifiers)) {
                    log.info("[Notification] Omitido: {} (Regla 2 - Actor Original)", user.getUsername());
                    continue;
                }
                finalRecipients.add(user.getUsername());
                log.info("[Notification] Añadido Admin: {} (Regla 3 - Flag Global)", user.getUsername());
            }

            log.debug("[Notification] Dispatching event {} to {} final recipients", context.eventType(), finalRecipients.size());

            String relativeTargetUrl = resolveTargetUrl(context.eventType().name(), context.projectId());

            for (String recipient : finalRecipients) {
                try {
                    inAppService.create(recipient, title, toInAppPlainText(message.body(), absoluteTargetUrl),
                            context.eventType().name(), template.getSeverity(), context.projectId(), relativeTargetUrl);
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "IN_APP", "SENT", null));
                } catch (Exception ex) {
                    log.warn("[Notification] In-app creation failed for recipient {} - {}", recipient, ex.getMessage());
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "IN_APP", "FAILED", ex.getMessage()));
                }

                try {
                    SeguridadUsuario resolvedRecipient = usuarioRepository.findByUsernameIgnoreCase(recipient)
                            .or(() -> usuarioRepository.findByCorreoIgnoreCase(recipient))
                            .orElse(null);
                    String emailAddress = resolveEmailAddress(resolvedRecipient, recipient);

                    boolean globalNotificationsEnabled = resolvedRecipient != null
                            && Boolean.TRUE.equals(resolvedRecipient.getRecibirNotificacionesGlobales());
                    boolean emailAllowed = globalNotificationsEnabled
                            || preferenceService.isEnabled(recipient, context.eventType().name(), context.projectId());

                    if (!emailAllowed) {
                        log.info("[Notification] Email omitido para {}: Preferencias apagadas", recipient);
                        mailDispatchTracker.skipped(recipient, message.subject(), "Preferencias de correo desactivadas");
                        continue;
                    }

                    NotificationMessage threadedMessage = message;
                    if (context.projectId() != null && !context.projectId().isBlank()) {
                        String projectName = String.valueOf(model.getOrDefault("projectName", context.projectId()));
                        threadedMessage = message.withThread(context.projectId(), projectName);
                    }
                    var result = sender.send(emailAddress, threadedMessage);
                    if (result.success()) {
                        log.debug("[Notification] Email sent to {}", emailAddress);
                        auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "EMAIL", result.status().name(), null));
                    } else {
                        auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "EMAIL", result.status().name(), result.errorMessage()));
                    }
                } catch (Exception ex) {
                    log.warn("[Notification] Email send failed for recipient {} - {}", recipient, ex.getMessage());
                    mailDispatchTracker.failed(recipient, message.subject(), ex.getMessage());
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "EMAIL", "FAILED", ex.getMessage()));
                }
            }
        } finally {
            MDC.clear();
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
        return usuarioRepository.findByUsernameIgnoreCase(username)
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
            text = text.replace(absoluteTargetUrl.replace("http://localhost:5173", ""), " ");
        }
        text = text.replaceAll("https?://\\S+", " ");
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 600 ? text.substring(0, 597).trim() + "..." : text;
    }
}
