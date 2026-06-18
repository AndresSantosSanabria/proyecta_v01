package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.notification.NotificationTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SpringNotificationTemplateRendererAdapter implements NotificationTemplateRendererPort {
    private final NotificationTemplateRenderer renderer;

    public SpringNotificationTemplateRendererAdapter(NotificationTemplateRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public NotificationMessage render(NotificationTemplate template, Map<String, Object> model) {
        return renderer.render(template, model);
    }
}
