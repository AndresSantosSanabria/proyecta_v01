package com.proyecta.api_gestion.service.notification;

import java.util.List;

public interface NotificationRecipientResolverPort {
    List<String> resolveRecipients(NotificationContext context);
}
