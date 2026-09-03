package com.proyecta.api_gestion.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationAuditDTO(
        UUID id,
        String eventCode,
        String recipient,
        String channel,
        String status,
        String failureReason,
        LocalDateTime createdAt
) {}
