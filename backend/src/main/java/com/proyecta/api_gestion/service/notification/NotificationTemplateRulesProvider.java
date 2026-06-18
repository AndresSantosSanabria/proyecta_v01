package com.proyecta.api_gestion.service.notification;

public interface NotificationTemplateRulesProvider {
    TemplateRules getRules(String eventCode);
}
