package com.proyecta.api_gestion.service.notification;

import java.util.Set;

public record TemplateRules(
        String eventCode,
        Set<String> allowedVariables,
        Set<String> requiredVariables
) {
}
