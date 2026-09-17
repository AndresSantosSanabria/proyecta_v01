package com.proyecta.api_gestion.service.scheduler;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.ViabilidadEstado;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActaConstitucionReminderService {

    private static final Logger log = LoggerFactory.getLogger(ActaConstitucionReminderService.class);

    private final ProyectoRepository proyectoRepository;
    private final NotificationEventPublisherPort notificationPublisher;

    public ActaConstitucionReminderService(
            ProyectoRepository proyectoRepository,
            NotificationEventPublisherPort notificationPublisher) {
        this.proyectoRepository = proyectoRepository;
        this.notificationPublisher = notificationPublisher;
    }

    @Scheduled(cron = "0 0 9 1 * *")
    public void enviarRecordatoriosCompletarProyecto() {
        log.info("Iniciando envio de recordatorios mensuales para completar proyectos");

        List<Proyecto> proyectosPendientes = proyectoRepository
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
        String directorUsername = proyecto.getDirector();
        if (directorUsername == null || directorUsername.isBlank()) {
            log.warn("Proyecto {} no tiene director asignado. Saltando notificacion.", proyecto.getId());
            return;
        }

        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), proyecto.getFechaLimiteCompletar());

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ACTA_CONSTITUCION_REMINDER,
                proyecto.getId(),
                "SYSTEM",
                java.util.Map.of(
                        "projectName", proyecto.getNombre(),
                        "diasRestantes", diasRestantes,
                        "recipients", List.of(directorUsername)
                )));
    }

    private void notificarPlazoVencido(Proyecto proyecto) {
        String directorUsername = proyecto.getDirector();
        String gestorUsername = proyecto.getRegistradoInicialPor();

        if (directorUsername != null && !directorUsername.isBlank()) {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.ACTA_CONSTITUCION_REMINDER,
                    proyecto.getId(),
                    "SYSTEM",
                    java.util.Map.of(
                            "projectName", proyecto.getNombre(),
                            "diasRestantes", 0L,
                            "plazoVencido", true,
                            "recipients", List.of(directorUsername)
                    )));
        }

        if (gestorUsername != null && !gestorUsername.isBlank()) {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.ACTA_CONSTITUCION_REMINDER,
                    proyecto.getId(),
                    "SYSTEM",
                    java.util.Map.of(
                            "projectName", proyecto.getNombre(),
                            "diasRestantes", 0L,
                            "plazoVencido", true,
                            "notificarGestor", true,
                            "recipients", List.of(gestorUsername)
                    )));
        }
    }
}
