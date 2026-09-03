package com.proyecta.api_gestion.dto.notification;

import java.time.Instant;
import java.util.UUID;

public record NotificationDispatchLogDTO(
        UUID id,
        String recipient,
        String subject,
        String status,
        String detail,
        Instant createdAt
) {}
