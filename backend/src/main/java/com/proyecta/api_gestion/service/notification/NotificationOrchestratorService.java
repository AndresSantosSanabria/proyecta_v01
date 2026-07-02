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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestratorService.class);

    private final NotificationRecipientResolverPort recipientResolver;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateRendererPort renderer;
    private final NotificationSenderPort sender;
    private final InAppNotificationService inAppService;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final NotificationAuditRepository auditRepository;

    public NotificationOrchestratorService(NotificationRecipientResolverPort recipientResolver,
                                           NotificationTemplateService templateService,
                                           NotificationPreferenceService preferenceService,
                                           NotificationTemplateRendererPort renderer,
                                           NotificationSenderPort sender,
                                           InAppNotificationService inAppService,
                                           SeguridadUsuarioRepository usuarioRepository,
                                           NotificationAuditRepository auditRepository) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
        this.sender = sender;
        this.inAppService = inAppService;
        this.usuarioRepository = usuarioRepository;
        this.auditRepository = auditRepository;
    }

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

            // 1. Contextual recipients
            List<String> contextualRecipients = recipientResolver.resolveRecipients(context);
            Set<String> finalRecipients = new HashSet<>();

            // 2. Anti-notification Rule (exclude actor)
            for (String rec : contextualRecipients) {
                if (rec == null || rec.isBlank()) continue;
                if (rec.equalsIgnoreCase(context.actorUsername())) {
                    log.info("[Notification] Omitido: {} (Regla 2 - Actor Original)", rec);
                    continue;
                }
                finalRecipients.add(rec);
            }

            // 3. Global Admin Inclusion (ignoring context)
            List<SeguridadUsuario> globalAdmins = usuarioRepository.findByRecibirNotificacionesGlobalesTrue();
            for (SeguridadUsuario admin : globalAdmins) {
                if (admin.getUsername().equalsIgnoreCase(context.actorUsername())) {
                    log.info("[Notification] Omitido Admin: {} (Regla 2 - Actor Original)", admin.getUsername());
                    continue;
                }
                finalRecipients.add(admin.getUsername());
                log.info("[Notification] Añadido Admin: {} (Regla 3 - Flag Global)", admin.getUsername());
            }

            log.debug("[Notification] Dispatching event {} to {} final recipients", context.eventType(), finalRecipients.size());

            // 4. Dispatch with Audit
            for (String recipient : finalRecipients) {
                // In-App
                try {
                    inAppService.create(recipient, title, message.body(),
                            context.eventType().name(), template.getSeverity(), context.projectId());
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "IN_APP", "SENT", null));
                } catch (Exception ex) {
                    log.warn("[Notification] In-app creation failed for recipient {} - {}", recipient, ex.getMessage());
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "IN_APP", "FAILED", ex.getMessage()));
                }

                // Email
                if (context.eventType() == NotificationEventType.PROJECT_DELAYED) {
                    continue;
                }

                try {
                    if (!preferenceService.isEnabled(recipient, context.eventType().name(), context.projectId())) {
                        log.info("[Notification] Email omitido para {}: Preferencias apagadas", recipient);
                        continue;
                    }
                    sender.send(recipient, message);
                    log.debug("[Notification] Email sent to {}", recipient);
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "EMAIL", "SENT", null));
                } catch (Exception ex) {
                    log.warn("[Notification] Email send failed for recipient {} - {}", recipient, ex.getMessage());
                    auditRepository.save(new NotificationAudit(context.eventType().name(), recipient, "EMAIL", "FAILED", ex.getMessage()));
                }
            }
        } finally {
            MDC.clear();
        }
    }
}
