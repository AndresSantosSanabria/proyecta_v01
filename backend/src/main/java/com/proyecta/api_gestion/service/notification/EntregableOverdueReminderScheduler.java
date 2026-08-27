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
public class EntregableOverdueReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(EntregableOverdueReminderScheduler.class);
    private static final String EVENT_CODE = NotificationEventType.ENTREGABLE_OVERDUE_REMINDER.name();

    private static final int DEFAULT_INTERVAL_DAYS = 8;

    private final EntregableRepository entregableRepository;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationEventPublisherPort notificationPublisher;
    private final SystemParameterService systemParameterService;

    public EntregableOverdueReminderScheduler(
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

    @Scheduled(cron = "${notifications.overdue-reminder.cron:0 0 9 * * *}")
    @Transactional
    public void scanAndRemindOverdueDeliverables() {
        int intervalDays = systemParameterService.getInt(
                SystemParameterKeys.NOTIFICATION_OVERDUE_REMINDER_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS);

        LocalDate today = LocalDate.now();
        LocalDateTime cutoffForDedup = today.minusDays(intervalDays).atStartOfDay();

        List<Entregable> vencidos = entregableRepository.findVencidosNoEntregados(today);

        for (Entregable entregable : vencidos) {
            try {
                procesarEntregable(entregable, today, cutoffForDedup);
            } catch (Exception ex) {
                log.warn("[OverdueReminder] Failed to process entregable {}: {}",
                        entregable.getId(), ex.getMessage());
            }
        }

        log.info("[OverdueReminder] Scanned {} overdue deliverables, interval={} days",
                vencidos.size(), intervalDays);
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

        long diasVencido = ChronoUnit.DAYS.between(entregable.getFechaLimite(), today);
        String projectName = entregable.getHito().getFase().getProyecto().getNombre();

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ENTREGABLE_OVERDUE_REMINDER,
                projectId,
                "system",
                java.util.Map.of(
                        "entregableNombre", entregable.getNombre(),
                        "entregableId", entregable.getId(),
                        "projectName", projectName,
                        "diasVencido", diasVencido,
                        "fechaLimite", entregable.getFechaLimite().toString(),
                        "recipients", List.of(director),
                        "title", "Entregable vencido hace " + diasVencido + " días: " + entregable.getNombre()
                )));
    }
}
