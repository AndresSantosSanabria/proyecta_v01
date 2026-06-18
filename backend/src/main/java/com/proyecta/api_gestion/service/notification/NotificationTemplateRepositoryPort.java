package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;

public interface NotificationTemplateRepositoryPort {
    NotificationTemplate findByEventCode(String eventCode);
}
