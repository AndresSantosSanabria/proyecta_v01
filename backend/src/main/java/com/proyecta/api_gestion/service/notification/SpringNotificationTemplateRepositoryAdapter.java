package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import org.springframework.stereotype.Service;

@Service
public class SpringNotificationTemplateRepositoryAdapter implements NotificationTemplateRepositoryPort {
    private final NotificationTemplateService service;

    public SpringNotificationTemplateRepositoryAdapter(NotificationTemplateService service) {
        this.service = service;
    }

    @Override
    public NotificationTemplate findByEventCode(String eventCode) {
        return service.get(eventCode);
    }
}
