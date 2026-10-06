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
public class EntregableOverdueReminderScheduler extends EntregableNotificationSchedulerSupport {

    private static final Logger log = LoggerFactory.getLogger(EntregableOverdueReminderScheduler.class);

    private static final int DEFAULT_INTERVAL_DAYS = 8;

    public EntregableOverdueReminderScheduler(
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
        return NotificationEventType.ENTREGABLE_OVERDUE_REMINDER;
    }

    @Override
    protected long calcularDias(LocalDate today, Entregable entregable) {
        return diasEntre(entregable.getFechaLimite(), today);
    }

    @Override
    protected String claveDias() {
        return "diasVencido";
    }

    @Override
    protected String tituloNotificacion(long dias, Entregable entregable) {
        return "Entregable vencido hace " + dias + " días: " + entregable.getNombre();
    }

    @Scheduled(cron = "${notifications.overdue-reminder.cron:0 0 9 * * *}")
    @Transactional
    public void scanAndRemindOverdueDeliverables() {
        int intervalDays = systemParameterService.getInt(
                SystemParameterKeys.NOTIFICATION_OVERDUE_REMINDER_INTERVAL_DAYS, DEFAULT_INTERVAL_DAYS);

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime cutoffForDedup = today.minusDays(intervalDays).atStartOfDay();

        List<Entregable> vencidos = entregableRepositoryPort.findVencidosNoEntregados(today);
        procesarCada(vencidos, today, cutoffForDedup, "OverdueReminder");

        log.info("[OverdueReminder] Scanned {} overdue deliverables, interval={} days",
                vencidos.size(), intervalDays);
    }
}
