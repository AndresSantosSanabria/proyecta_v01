package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(example = """
    {"id":"8b2d4f6a-1c9e-4a3b-9d7f-abcdefabcdef","recipient":"maria.gomez@proyecta.gov.co","subject":"[Proyecta] Entregable próximo a vencer","status":"SENT","detail":"Mensaje enviado correctamente","createdAt":"2026-07-15T10:30:00Z"}
    """)
public record NotificationDispatchLogDTO(
        UUID id,
        String recipient,
        String subject,
        String status,
        String detail,
        Instant createdAt
) {}
