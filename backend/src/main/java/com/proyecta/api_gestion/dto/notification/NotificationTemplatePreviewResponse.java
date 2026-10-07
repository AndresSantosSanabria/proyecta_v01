package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"subject":"[Proyecta] Entregable próximo a vencer: Documento de alcance aprobado","body":"<p>El entregable Documento de alcance aprobado vence el 2026-07-20</p>","htmlEnabled":true}
    """)
public record NotificationTemplatePreviewResponse(
        String subject,
        String body,
        Boolean htmlEnabled
) {
}
