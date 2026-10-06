package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.application.readmodel.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.application.port.out.persistence.ProyectoRepositoryPort;
import com.proyecta.api_gestion.application.port.out.persistence.security.SeguridadUsuarioRepositoryPort;
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
public class ProjectDelayNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProjectDelayNotificationScheduler.class);
    private static final String EVENT_CODE = NotificationEventType.PROJECT_DELAYED.name();

    private final ProyectoRepositoryPort proyectoRepositoryPort;
    private final SeguridadUsuarioRepositoryPort usuarioRepositoryPort;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationEventPublisherPort notificationPublisher;

    public ProjectDelayNotificationScheduler(ProyectoRepositoryPort proyectoRepositoryPort,
                                             SeguridadUsuarioRepositoryPort usuarioRepositoryPort,
                                             InAppNotificationService inAppNotificationService,
                                             NotificationEventPublisherPort notificationPublisher) {
        this.proyectoRepositoryPort = proyectoRepositoryPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.inAppNotificationService = inAppNotificationService;
        this.notificationPublisher = notificationPublisher;
    }

    @Scheduled(cron = "${notifications.project-delay.cron:0 0 */6 * * *}")
    @Transactional
    public void scanAndNotifyDelayedProjects() {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDateTime dayStart = today.atStartOfDay();

        List<DashboardProjectSummaryDTO> delayedProjects = proyectoRepositoryPort.getDashboardProjectSummary(today).stream()
                .filter(project -> project.getEntregablesAtrasados() > 0)
                .toList();

        for (DashboardProjectSummaryDTO project : delayedProjects) {
            notificarProyectoRetrasado(project, dayStart);
        }
    }

    private void notificarProyectoRetrasado(DashboardProjectSummaryDTO project, LocalDateTime dayStart) {
        String projectId = project.getCodigo();
        var proyectoOpt = proyectoRepositoryPort.findById(projectId);
        if (proyectoOpt.isEmpty()) {
            return;
        }
        var proyecto = proyectoOpt.get();
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) {
            return;
        }

        String director = proyecto.getCorreoDirector();
        if (director == null || director.isBlank()) {
            return;
        }

        var recipient = usuarioRepositoryPort.findByUsernameIgnoreCase(director)
                .or(() -> usuarioRepositoryPort.findByCorreoIgnoreCase(director))
                .orElse(null);
        if (recipient == null) {
            return;
        }

        if (inAppNotificationService.existsForRecipient(recipient.getId(), EVENT_CODE, projectId, dayStart)) {
            return;
        }

        try {
            notificationPublisher.publish(new NotificationContext(
                    NotificationEventType.PROJECT_DELAYED,
                    projectId,
                    "system",
                    java.util.Map.of(
                            "projectName", project.getNombreProyecto(),
                            "overdueDeliverables", project.getEntregablesAtrasados(),
                            "recipients", recipients,
                            "title", "Proyecto con retrasos: " + project.getNombreProyecto()
                    )));
        } catch (Exception ex) {
            log.warn("[Notification] Failed to create delayed-project notification for {}: {}", projectId, ex.getMessage());
        }
    }
}
