package com.proyecta.api_gestion.service.notification;

import java.util.Map;

public record NotificationContext(
        NotificationEventType eventType,
        String projectId,
        String actorUsername,
        Map<String, Object> attributes
) {
}
