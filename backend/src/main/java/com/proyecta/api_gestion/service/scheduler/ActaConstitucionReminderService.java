package com.proyecta.api_gestion.service.scheduler;

import com.proyecta.api_gestion.domain.model.Proyecto;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActaConstitucionReminderService {

    private static final Logger log = LoggerFactory.getLogger(ActaConstitucionReminderService.class);

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final NotificationEventPublisherPort notificationPublisher;

    public ActaConstitucionReminderService(
            ProyectoRepositoryPort proyectoRepositoryPort,
            NotificationEventPublisherPort notificationPublisher) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.notificationPublisher = notificationPublisher;
    }

    @Scheduled(cron = "0 0 9 1 * *")
    public void enviarRecordatoriosCompletarProyecto() {
        log.info("Iniciando envio de recordatorios mensuales para completar proyectos");

        List<Proyecto> proyectosPendientes = proyectoRepositoryPort
                .findByDocumentosVerificadosTrueAndRequiereCompletitudDirectorTrueAndCierreForzosoFalse();

        for (Proyecto proyecto : proyectosPendientes) {
            try {
                if (proyecto.plazoCompletarVencido()) {
                    notificarPlazoVencido(proyecto);
                } else {
                    notificarCompletarProyecto(proyecto);
                }
            } catch (Exception e) {
                log.error("Error enviando recordatorio de completar proyecto {}: {}", proyecto.getId(), e.getMessage());
            }
        }

        log.info("Finalizado envio de recordatorios. Proyectos notificados: {}", proyectosPendientes.size());
    }

    private void notificarCompletarProyecto(Proyecto proyecto) {
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) {
            log.warn("Proyecto {} no tiene destinatarios (gestor/director). Saltando notificacion.", proyecto.getId());
            return;
        }

        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(java.time.ZoneId.systemDefault()), proyecto.getFechaLimiteCompletar());

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ACTA_CONSTITUCION_REMINDER,
                proyecto.getId(),
                "SYSTEM",
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "diasRestantes", diasRestantes,
                        "recipients", recipients
                )));
    }

    private void notificarPlazoVencido(Proyecto proyecto) {
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) {
            return;
        }

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ACTA_CONSTITUCION_REMINDER,
                proyecto.getId(),
                "SYSTEM",
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "diasRestantes", 0L,
                        "plazoVencido", true,
                        "recipients", recipients
                )));
    }
}
