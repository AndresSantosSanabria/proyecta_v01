package com.proyecta.api_gestion.dto.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationPreferenceUpdateRequest(
        @NotBlank String username,
        @NotBlank String eventCode,
        String projectId,
        Boolean enabled,
        Boolean emailEnabled
) {
}
