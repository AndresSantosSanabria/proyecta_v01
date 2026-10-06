package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.domain.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.application.port.out.persistence.notification.NotificationTemplateRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class NotificationTemplateService {
    private final NotificationTemplateRepositoryPort templateRepositoryPort;
    private final TemplateVariableValidator variableValidator;

    public NotificationTemplateService(NotificationTemplateRepositoryPort templateRepositoryPort, TemplateVariableValidator variableValidator) {
        this.templateRepositoryPort = templateRepositoryPort;
        this.variableValidator = variableValidator;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> listAll() {
        return templateRepositoryPort.findAll();
    }

    @Transactional(readOnly = true)
    public NotificationTemplate get(String eventCode) {
        return templateRepositoryPort.findById(eventCode)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada para evento: " + eventCode));
    }

    @Transactional
    public NotificationTemplate upsert(NotificationTemplate template, String updatedBy) {
        variableValidator.validate(template.getEventCode(), template.getSubjectTemplate(), template.getBodyTemplate());
        template.setUpdatedBy(updatedBy);
        template.setUpdatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        return templateRepositoryPort.save(template);
    }
}
