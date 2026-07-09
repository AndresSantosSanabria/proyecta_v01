package com.proyecta.api_gestion.dto.notification;

import java.time.LocalDateTime;

public record InAppNotificationDTO(
        Long id,
        String title,
        String message,
        String eventCode,
        Boolean readStatus,
        LocalDateTime createdAt,
        String severity,
        String targetUrl
) {
}
