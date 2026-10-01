package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(example = """
    {"id":"6f1c9a3e-2b7d-4c1a-8e5f-1234567890ab","eventCode":"PROJECT_DELIVERABLE_DUE","recipient":"maria.gomez@proyecta.gov.co","channel":"EMAIL","status":"SENT","failureReason":null,"createdAt":"2026-07-15T10:30:00"}
    """)
public record NotificationAuditDTO(
        UUID id,
        String eventCode,
        String recipient,
        String channel,
        String status,
        String failureReason,
        LocalDateTime createdAt
) {}
