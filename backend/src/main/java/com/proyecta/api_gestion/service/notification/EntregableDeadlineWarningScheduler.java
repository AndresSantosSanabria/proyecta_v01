package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.domain.model.Entregable;
import com.proyecta.api_gestion.application.port.out.persistence.EntregableRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class EntregableDeadlineWarningScheduler extends EntregableNotificationSchedulerSupport {

    private static final Logger log = LoggerFactory.getLogger(EntregableDeadlineWarningScheduler.class);

    private static final int DEFAULT_INTERVAL_DAYS = 60;

    public EntregableDeadlineWarningScheduler(
            EntregableRepositoryPort entregableRepositoryPort,
            SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
            InAppNotificationService inAppNotificationService,
            NotificationEventPublisherPort notificationPublisher,
            SystemParameterService systemParameterService) {
        super(entregableRepositoryPort, usuarioRepositoryPort, inAppNotificationService,
                notificationPublisher, systemParameterService);
    }

    @Override
    protected NotificationEventType eventType() {
        return NotificationEventType.ENTREGABLE_DEADLINE_WARNING;
    }

    @Override
    protected long calcularDias(LocalDate today, Entregable entregable) {
        return diasEntre(today, entregable.getFechaLimite());
    }

    @Override
    protected String claveDias() {
        return "diasRestantes";
    }

    @Override
    protected String tituloNotificacion(long dias, Entregable entregable) {
        return "Entregable vence en " + dias + " días: " + entregable.getNombre();
    }

    @Scheduled(cron = "${notifications.deadline-warning.cron:0 0 8 * * *}")
    @Transactional
    public void scanAndNotifyApproachingDeadlines() {
        int intervalDays = systemParameterService.getInt(
                SystemParameterKeys.NOTIFICATION_DEADLINE_WARNING_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime cutoffForDedup = today.minusDays(intervalDays).atStartOfDay();

        List<Entregable> noCompletados = entregableRepositoryPort.findNoCompletados(today);
        procesarCada(noCompletados, today, cutoffForDedup, "DeadlineWarning");

        log.info("[DeadlineWarning] Scanned {} active deliverables, interval={} days",
                noCompletados.size(), intervalDays);
    }
}
