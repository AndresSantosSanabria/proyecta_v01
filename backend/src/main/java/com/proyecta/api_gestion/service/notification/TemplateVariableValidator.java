package com.proyecta.api_gestion.service.notification;

public interface TemplateVariableValidator {
    void validate(String eventCode, String subjectTemplate, String bodyTemplate);
}
