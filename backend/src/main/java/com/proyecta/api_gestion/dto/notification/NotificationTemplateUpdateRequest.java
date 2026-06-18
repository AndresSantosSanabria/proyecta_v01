package com.proyecta.api_gestion.dto.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationTemplateUpdateRequest(
        @NotBlank String eventCode,
        Boolean enabled,
        Boolean htmlEnabled,
        String severity,
        String scope,
        @NotBlank String subjectTemplate,
        @NotBlank String bodyTemplate
) {
}
