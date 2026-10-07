package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"eventCode":"PROJECT_DELIVERABLE_DUE","eventName":"Entregable próximo a vencer","enabled":true,"htmlEnabled":true,"severity":"WARNING","scope":"PROJECT","subjectTemplate":"[Proyecta] Entregable próximo a vencer: {{entregableNombre}}","bodyTemplate":"<p>El entregable {{entregableNombre}} vence el {{fechaLimite}}</p>","targetRoles":"DIRECTOR,COORDINADOR","updatedBy":"admin@proyecta.gov.co","category":"CRONOGRAMA"}
    """)
public record NotificationTemplateDTO(
        String eventCode,
        String eventName,
        Boolean enabled,
        Boolean htmlEnabled,
        String severity,
        String scope,
        String subjectTemplate,
        String bodyTemplate,
        String targetRoles,
        String updatedBy,
        String category
) {
}
