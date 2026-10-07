package com.proyecta.api_gestion.service.notification;

public interface NotificationSenderPort {
    NotificationSendResult send(String to, NotificationMessage message);
}
