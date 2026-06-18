package com.proyecta.api_gestion.service.notification;

public interface NotificationEventPublisherPort {
    void publish(NotificationContext context);
}
