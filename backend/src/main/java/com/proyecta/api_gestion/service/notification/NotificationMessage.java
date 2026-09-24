package com.proyecta.api_gestion.service.notification;

public record NotificationMessage(String subject, String body, boolean html, String projectId, String threadTopic) {

    public NotificationMessage(String subject, String body, boolean html) {
        this(subject, body, html, null, null);
    }

    public NotificationMessage withThread(String projectId, String threadTopic) {
        return new NotificationMessage(subject, body, html, projectId, threadTopic);
    }
}
