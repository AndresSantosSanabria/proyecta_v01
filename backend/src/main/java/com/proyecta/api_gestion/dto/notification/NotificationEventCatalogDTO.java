package com.proyecta.api_gestion.dto.notification;

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
