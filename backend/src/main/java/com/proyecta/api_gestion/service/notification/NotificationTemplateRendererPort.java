package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;

import java.util.Map;

public interface NotificationTemplateRendererPort {
    NotificationMessage render(NotificationTemplate template, Map<String, Object> model);
}
