package com.proyecta.api_gestion.service.notification;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DefaultNotificationRecipientResolver implements NotificationRecipientResolverPort {
    @Override
    public List<String> resolveRecipients(NotificationContext context) {
        Object recipients = context.attributes() == null ? null : context.attributes().get("recipients");
        if (recipients instanceof List<?> list) {
            List<String> resolved = new ArrayList<>();
            for (Object value : list) {
                if (value != null) {
                    resolved.add(String.valueOf(value));
                }
            }
            return resolved;
        }
        return List.of();
    }
}
