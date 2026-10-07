package com.proyecta.api_gestion.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(example = """
    {"code":"PROJECT_DELIVERABLE_DUE","name":"Entregable próximo a vencer","description":"Notifica cuando un entregable está por vencer dentro de la ventana configurada","category":"CRONOGRAMA","defaultEnabled":true,"active":true,"requiresProjectContext":true}
    """)
public record NotificationEventCatalogDTO(
        String code,
        String name,
        String description,
        String category,
        Boolean defaultEnabled,
        Boolean active,
        Boolean requiresProjectContext
) {
}
