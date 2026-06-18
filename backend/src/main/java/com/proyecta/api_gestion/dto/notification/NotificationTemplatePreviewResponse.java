package com.proyecta.api_gestion.dto.notification;

public record NotificationTemplatePreviewResponse(
        String subject,
        String body,
        Boolean htmlEnabled
) {
}
