package com.proyecta.api_gestion.dto.notification;

import jakarta.validation.constraints.NotBlank;

public record ProjectEmailNotificationRequest(
        @NotBlank String mensaje
) {
}
