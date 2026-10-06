package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.domain.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.application.port.out.persistence.notification.InAppNotificationRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
public class ProjectDelayNotificationService {

    private static final String EVENT_CODE = NotificationEventType.PROJECT_DELAYED.name();

    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;
    private final InAppNotificationRepositoryPort inAppNotificationRepositoryPort;
    private final NotificationEventPublisherPort notificationPublisher;

    public ProjectDelayNotificationService(SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
                                           InAppNotificationRepositoryPort inAppNotificationRepositoryPort,
                                           NotificationEventPublisherPort notificationPublisher) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.inAppNotificationRepositoryPort = inAppNotificationRepositoryPort;
        this.notificationPublisher = notificationPublisher;
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void notifyIfDelayed(Proyecto proyecto, ProyectoAvanceResponseDTO snapshot, String actorUsername) {
        if (proyecto == null || snapshot == null || proyecto.getId() == null) {
            return;
        }

        if (!"ATRASO".equalsIgnoreCase(snapshot.estado())) {
            return;
        }

        String director = proyecto.getCorreoDirector();
        if (director == null || director.isBlank()) {
            return;
        }

        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) {
            return;
        }

        SeguridadUsuario recipient = usuarioRepositoryPort.findByUsernameIgnoreCase(director)
                .or(() -> usuarioRepositoryPort.findByCorreoIgnoreCase(director))
                .orElse(null);
        if (recipient == null) {
            return;
        }

        LocalDateTime dayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay();
        boolean alreadyNotified = inAppNotificationRepositoryPort.existsByRecipientIdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(
                recipient.getId(),
                EVENT_CODE,
                proyecto.getId(),
                dayStart);
        if (alreadyNotified) {
            return;
        }

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.PROJECT_DELAYED,
                proyecto.getId(),
                actorUsername == null || actorUsername.isBlank() ? "system" : actorUsername,
                Map.of(
                        "projectName", proyecto.getNombre(),
                        "overdueDeliverables", snapshot.entregablesAtrasados(),
                        "recipients", recipients,
                        "title", "Proyecto con retrasos: " + proyecto.getNombre()
                )));
    }
}
