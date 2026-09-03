package com.proyecta.api_gestion.dto.notification;

public record NotificationTemplateDTO(
        String eventCode,
        Boolean enabled,
        Boolean htmlEnabled,
        String severity,
        String scope,
        String subjectTemplate,
        String bodyTemplate,
        String targetRoles,
        String updatedBy,
        String category
) {
}
