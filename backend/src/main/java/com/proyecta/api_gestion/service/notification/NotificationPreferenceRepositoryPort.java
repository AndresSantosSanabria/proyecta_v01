package com.proyecta.api_gestion.service.notification;

public interface NotificationPreferenceRepositoryPort {
    boolean isEnabled(String username, String eventCode, String projectId);
}
