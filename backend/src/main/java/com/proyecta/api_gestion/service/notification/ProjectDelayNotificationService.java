package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.security.SeguridadUsuario;
import com.proyecta.api_gestion.repository.notification.InAppNotificationRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ProjectDelayNotificationService {

    private static final String EVENT_CODE = NotificationEventType.PROJECT_DELAYED.name();

    private final SeguridadUsuarioRepository usuarioRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final NotificationEventPublisherPort notificationPublisher;

    public ProjectDelayNotificationService(SeguridadUsuarioRepository usuarioRepository,
                                           InAppNotificationRepository inAppNotificationRepository,
                                           NotificationEventPublisherPort notificationPublisher) {
        this.usuarioRepository = usuarioRepository;
        this.inAppNotificationRepository = inAppNotificationRepository;
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

        SeguridadUsuario recipient = usuarioRepository.findByUsernameIgnoreCase(director)
                .or(() -> usuarioRepository.findByCorreoIgnoreCase(director))
                .orElse(null);
        if (recipient == null) {
            return;
        }

        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        boolean alreadyNotified = inAppNotificationRepository.existsByRecipient_IdAndEventCodeAndSourceEntityIdAndCreatedAtAfter(
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
                        "recipients", List.of(director),
                        "title", "Proyecto " + proyecto.getId() + " con retrasos"
                )));
    }
}
