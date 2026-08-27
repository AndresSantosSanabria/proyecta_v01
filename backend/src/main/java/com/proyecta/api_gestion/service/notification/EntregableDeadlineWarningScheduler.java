package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.model.Entregable;
import com.proyecta.api_gestion.repository.EntregableRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EntregableDeadlineWarningScheduler {

    private static final Logger log = LoggerFactory.getLogger(EntregableDeadlineWarningScheduler.class);
    private static final String EVENT_CODE = NotificationEventType.ENTREGABLE_DEADLINE_WARNING.name();

    private static final int DEFAULT_INTERVAL_DAYS = 60;

    private final EntregableRepository entregableRepository;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationEventPublisherPort notificationPublisher;
    private final SystemParameterService systemParameterService;

    public EntregableDeadlineWarningScheduler(
            EntregableRepository entregableRepository,
            SeguridadUsuarioRepository usuarioRepository,
            InAppNotificationService inAppNotificationService,
            NotificationEventPublisherPort notificationPublisher,
            SystemParameterService systemParameterService) {
        this.entregableRepository = entregableRepository;
        this.usuarioRepository = usuarioRepository;
        this.inAppNotificationService = inAppNotificationService;
        this.notificationPublisher = notificationPublisher;
        this.systemParameterService = systemParameterService;
    }

    @Scheduled(cron = "${notifications.deadline-warning.cron:0 0 8 * * *}")
    @Transactional
    public void scanAndNotifyApproachingDeadlines() {
        int intervalDays = systemParameterService.getInt(
                SystemParameterKeys.NOTIFICATION_DEADLINE_WARNING_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS);

        LocalDate today = LocalDate.now();
        LocalDateTime cutoffForDedup = today.minusDays(intervalDays).atStartOfDay();

        List<Entregable> noCompletados = entregableRepository.findNoCompletados(today);

        for (Entregable entregable : noCompletados) {
            try {
                procesarEntregable(entregable, today, cutoffForDedup);
            } catch (Exception ex) {
                log.warn("[DeadlineWarning] Failed to process entregable {}: {}",
                        entregable.getId(), ex.getMessage());
            }
        }

        log.info("[DeadlineWarning] Scanned {} active deliverables, interval={} days",
                noCompletados.size(), intervalDays);
    }

    private void procesarEntregable(Entregable entregable, LocalDate today,
                                     LocalDateTime cutoffForDedup) {
        String projectId = entregable.getHito().getFase().getProyecto().getId();
        String director = entregable.getHito().getFase().getProyecto().getCorreoDirector();

        if (director == null || director.isBlank()) {
            return;
        }

        var recipient = usuarioRepository.findByUsernameIgnoreCase(director)
                .or(() -> usuarioRepository.findByCorreoIgnoreCase(director))
                .orElse(null);
        if (recipient == null) {
            return;
        }

        String sourceEntityId = String.valueOf(entregable.getId());
        if (inAppNotificationService.existsForRecipient(recipient.getId(), EVENT_CODE, sourceEntityId, cutoffForDedup)) {
            return;
        }

        long diasRestantes = ChronoUnit.DAYS.between(today, entregable.getFechaLimite());
        String projectName = entregable.getHito().getFase().getProyecto().getNombre();

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ENTREGABLE_DEADLINE_WARNING,
                projectId,
                "system",
                java.util.Map.of(
                        "entregableNombre", entregable.getNombre(),
                        "entregableId", entregable.getId(),
                        "projectName", projectName,
                        "diasRestantes", diasRestantes,
                        "fechaLimite", entregable.getFechaLimite().toString(),
                        "recipients", List.of(director),
                        "title", "Entregable vence en " + diasRestantes + " días: " + entregable.getNombre()
                )));
    }
}
