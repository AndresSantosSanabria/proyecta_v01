package com.proyecta.api_gestion.dto.notification;

import java.util.Map;

public record NotificationTemplatePreviewRequest(
        String subjectTemplate,
        String bodyTemplate,
        Boolean htmlEnabled,
        Map<String, Object> variables
) {
}
