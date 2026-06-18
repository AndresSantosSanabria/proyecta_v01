package com.proyecta.api_gestion.service.notification;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class SpringNotificationEventPublisher implements NotificationEventPublisherPort {
    private final ApplicationEventPublisher publisher;

    public SpringNotificationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(NotificationContext context) {
        publisher.publishEvent(context);
    }
}
