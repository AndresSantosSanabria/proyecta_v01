package com.proyecta.api_gestion.service.notification;

import org.springframework.stereotype.Service;

@Service
public class DefaultNotificationPreferenceRepositoryAdapter implements NotificationPreferenceRepositoryPort {
    private final NotificationPreferenceService service;

    public DefaultNotificationPreferenceRepositoryAdapter(NotificationPreferenceService service) {
        this.service = service;
    }

    @Override
    public boolean isEnabled(String username, String eventCode, String projectId) {
        return service.isEnabled(username, eventCode, projectId);
    }
}
