package com.proyecta.api_gestion.dto.notification;

public record NotificationPreferenceDTO(
        Long id,
        String username,
        String eventCode,
        String projectId,
        Boolean enabled,
        Boolean emailEnabled
) {
}
