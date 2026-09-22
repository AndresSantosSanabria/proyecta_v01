package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.Proyecto;

import java.util.ArrayList;
import java.util.List;

public final class ProjectNotificationRecipients {

    private ProjectNotificationRecipients() {
    }

    public static List<String> resolve(Proyecto proyecto) {
        if (proyecto == null) {
            return List.of();
        }
        List<String> recipients = new ArrayList<>();
        String gestor = proyecto.getRegistradoInicialPor();
        if (gestor != null && !gestor.isBlank()) {
            recipients.add(gestor.trim());
        }
        String directorEmail = proyecto.getCorreoDirector();
        if (directorEmail != null && !directorEmail.isBlank()) {
            recipients.add(directorEmail.trim());
        }
        return recipients;
    }
}
