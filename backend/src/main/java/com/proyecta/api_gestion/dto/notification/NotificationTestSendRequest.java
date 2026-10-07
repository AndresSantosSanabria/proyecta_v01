package com.proyecta.api_gestion.dto.notification;

import jakarta.validation.constraints.NotBlank;

public record NotificationTestSendRequest(
        @NotBlank String eventCode,
        @NotBlank String subjectTemplate,
        @NotBlank String bodyTemplate,
        Boolean htmlEnabled,
        java.util.Map<String, Object> variables
) {
}
