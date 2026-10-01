package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(example = """
    {"id":9501,"title":"Entregable próximo a vencer","message":"El entregable 'Documento de alcance aprobado' vence en 3 días","eventCode":"PROJECT_DELIVERABLE_DUE","readStatus":false,"createdAt":"2026-07-15T10:30:00","severity":"WARNING","targetUrl":"/proyectos/PROY-CUN-2026-008"}
    """)
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
