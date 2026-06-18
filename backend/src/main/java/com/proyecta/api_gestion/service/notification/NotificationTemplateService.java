package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import com.proyecta.api_gestion.repository.notification.NotificationTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationTemplateService {
    private final NotificationTemplateRepository templateRepository;
    private final TemplateVariableValidator variableValidator;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository, TemplateVariableValidator variableValidator) {
        this.templateRepository = templateRepository;
        this.variableValidator = variableValidator;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplate> listAll() {
        return templateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public NotificationTemplate get(String eventCode) {
        return templateRepository.findById(eventCode)
                .orElseThrow(() -> new IllegalArgumentException("Plantilla no encontrada para evento: " + eventCode));
    }

    @Transactional
    public NotificationTemplate upsert(NotificationTemplate template, String updatedBy) {
        variableValidator.validate(template.getEventCode(), template.getSubjectTemplate(), template.getBodyTemplate());
        template.setUpdatedBy(updatedBy);
        template.setUpdatedAt(LocalDateTime.now());
        return templateRepository.save(template);
    }
}
