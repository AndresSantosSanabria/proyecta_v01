package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationAudit;
import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.notification.NotificationAuditRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
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

    private static final Map<String, String> EVENT_ROUTE_MAP;
    static {
        var tmp = new HashMap<String, String>();
        tmp.put("CLOSURE_REQUESTED", "/closure");
        tmp.put("CLOSURE_APPROVED", "/closure");
        tmp.put("CLOSURE_REJECTED", "/closure");
        tmp.put("PROJECT_CLOSED", "/closure");
        tmp.put("DELIVERABLE_EVIDENCE_UPLOADED", "/progress");
        tmp.put("DELIVERABLE_APPROVED", "/progress");
        tmp.put("DELIVERABLE_REJECTED", "/progress");
        tmp.put("OBSERVATION_SUBSANATED", "/progress");
        tmp.put("PROJECT_DELAYED", "/schedule");
        tmp.put("PROJECT_INITIAL_REGISTERED", "/progress");
        tmp.put("PROJECT_INITIAL_COMPLETED", "/progress");
        tmp.put("PROJECT_UPDATED", "/progress");
        tmp.put("PROJECT_DOCUMENT_UPLOADED", "/progress");
        tmp.put("PROJECT_DOCUMENT_DELETED", "/progress");
        tmp.put("PROJECT_BENEFIT_IMPACT_REQUIRED", "/progress");
        tmp.put("PROJECT_BENEFIT_IMPACT_SUBMITTED", "/progress");
        tmp.put("PROJECT_BENEFIT_IMPACT_RESUBMITTED", "/progress");
        tmp.put("PROJECT_BENEFIT_IMPACT_REVIEWED", "/progress");
        tmp.put("RISK_CREATED", "/risks");
        tmp.put("RISK_UPDATED", "/risks");
        tmp.put("RISK_TREATED", "/risks");
        tmp.put("ENTREGABLE_FECHA_CAMBIADA", "/schedule");
        tmp.put("ENTREGABLE_DEADLINE_WARNING", "/progress");
        tmp.put("ENTREGABLE_OVERDUE_REMINDER", "/progress");
        EVENT_ROUTE_MAP = Map.copyOf(tmp);
    }

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

    public NotificationOrchestratorService(NotificationRecipientResolverPort recipientResolver,
                                           NotificationTemplateService templateService,
                                           NotificationPreferenceService preferenceService,
                                           NotificationTemplateRendererPort renderer,
                                           NotificationSenderPort sender,
                                           InAppNotificationService inAppService,
                                           SeguridadUsuarioRepository usuarioRepository,
                                           NotificationAuditRepository auditRepository,
                                           NotificationMailDispatchTracker mailDispatchTracker,
                                           NotificationActorResolver actorResolver) {
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
            model.putIfAbsent("actorUsername", context.actorUsername());

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
                if (actorResolver.isActor(user.getUsername(), actorIdentifiers) || actorResolver.isActor(user.getCorreo(), actorIdentifiers)) {
                    log.info("[Notification] Omitido Admin: {} (Regla 2 - Actor Original)", user.getUsername());
                    continue;
                }
                finalRecipients.add(user.getUsername());
                log.info("[Notification] Añadido Admin: {} (Regla 3 - Flag Global)", user.getUsername());
            }

            log.debug("[Notification] Dispatching event {} to {} final recipients", context.eventType(), finalRecipients.size());

            String targetUrl = resolveTargetUrl(context.eventType().name(), context.projectId());

            for (String recipient : finalRecipients) {
                try {
                    inAppService.create(recipient, title, message.body(),
                            context.eventType().name(), template.getSeverity(), context.projectId(), targetUrl);
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

                    var result = sender.send(emailAddress, message);
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

    private String resolveTargetUrl(String eventCode, String projectId) {
        if (projectId == null || projectId.isBlank()) {
            return null;
        }
        String route = EVENT_ROUTE_MAP.get(eventCode);
        if (route == null) {
            return null;
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
}
