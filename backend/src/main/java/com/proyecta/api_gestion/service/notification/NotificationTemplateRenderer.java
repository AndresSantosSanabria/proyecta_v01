package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationTemplateRenderer {
    public NotificationMessage render(NotificationTemplate template, Map<String, Object> model) {
        String subject = apply(template.getSubjectTemplate(), model);
        String body = apply(template.getBodyTemplate(), model);
        return new NotificationMessage(subject, body, Boolean.TRUE.equals(template.getHtmlEnabled()));
    }

    public String apply(String template, Map<String, Object> model) {
        String result = template == null ? "" : template;
        if (model == null) {
            return result;
        }
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            result = result.replace(token, entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }
}
