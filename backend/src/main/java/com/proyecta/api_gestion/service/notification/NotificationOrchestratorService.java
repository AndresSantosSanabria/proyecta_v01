package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestratorService.class);

    private final NotificationRecipientResolverPort recipientResolver;
    private final NotificationTemplateService templateService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationTemplateRendererPort renderer;
    private final NotificationSenderPort sender;
    private final InAppNotificationService inAppService;

    public NotificationOrchestratorService(NotificationRecipientResolverPort recipientResolver,
                                           NotificationTemplateService templateService,
                                           NotificationPreferenceService preferenceService,
                                           NotificationTemplateRendererPort renderer,
                                           NotificationSenderPort sender,
                                           InAppNotificationService inAppService) {
        this.recipientResolver = recipientResolver;
        this.templateService = templateService;
        this.preferenceService = preferenceService;
        this.renderer = renderer;
        this.sender = sender;
        this.inAppService = inAppService;
    }

    public void dispatch(NotificationContext context) {
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

        List<String> recipients = recipientResolver.resolveRecipients(context);
        log.debug("[Notification] Dispatching event {} to {} recipients", context.eventType(), recipients.size());

        for (String recipient : recipients) {
            if (recipient == null || recipient.isBlank()) continue;
            try {
                // Always attempt in-app notification (best-effort, no throw)
                inAppService.create(recipient, title, message.body(),
                        context.eventType().name(), template.getSeverity(), context.projectId());
            } catch (Exception ex) {
                log.warn("[Notification] In-app creation failed for recipient {} - {}", recipient, ex.getMessage());
            }

            if (context.eventType() == NotificationEventType.PROJECT_DELAYED) {
                continue;
            }

            try {
                // Email only if user preferences allow it
                if (!preferenceService.isEnabled(recipient, context.eventType().name(), context.projectId())) continue;
                sender.send(recipient, message);
                log.debug("[Notification] Email sent to {}", recipient);
            } catch (Exception ex) {
                log.warn("[Notification] Email send failed for recipient {} - {}", recipient, ex.getMessage());
            }
        }
    }
}
