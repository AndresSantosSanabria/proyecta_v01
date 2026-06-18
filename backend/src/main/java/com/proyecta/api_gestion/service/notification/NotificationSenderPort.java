package com.proyecta.api_gestion.service.notification;

public interface NotificationSenderPort {
    void send(String to, NotificationMessage message);
}
