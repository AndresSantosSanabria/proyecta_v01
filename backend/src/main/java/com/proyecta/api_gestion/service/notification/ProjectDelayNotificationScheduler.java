package com.proyecta.api_gestion.service.notification;

import com.proyecta.api_gestion.dto.dashboard.DashboardProjectSummaryDTO;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.security.SeguridadUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectDelayNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProjectDelayNotificationScheduler.class);
    private static final String EVENT_CODE = NotificationEventType.PROJECT_DELAYED.name();

    private final ProyectoRepository proyectoRepository;
    private final SeguridadUsuarioRepository usuarioRepository;
    private final InAppNotificationService inAppNotificationService;
    private final NotificationEventPublisherPort notificationPublisher;

    public ProjectDelayNotificationScheduler(ProyectoRepository proyectoRepository,
                                             SeguridadUsuarioRepository usuarioRepository,
                                             InAppNotificationService inAppNotificationService,
                                             NotificationEventPublisherPort notificationPublisher) {
        this.proyectoRepository = proyectoRepository;
        this.usuarioRepository = usuarioRepository;
        this.inAppNotificationService = inAppNotificationService;
        this.notificationPublisher = notificationPublisher;
    }

    @Scheduled(cron = "${notifications.project-delay.cron:0 0 */6 * * *}")
    @Transactional
    public void scanAndNotifyDelayedProjects() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();

        List<DashboardProjectSummaryDTO> delayedProjects = proyectoRepository.getDashboardProjectSummary(today).stream()
                .filter(project -> project.getEntregablesAtrasados() > 0)
                .toList();

        for (DashboardProjectSummaryDTO project : delayedProjects) {
            String projectId = project.getCodigo();
            String director = proyectoRepository.findById(projectId)
                    .map(proyecto -> proyecto.getCorreoDirector())
                    .orElse(null);
            if (director == null || director.isBlank()) {
                continue;
            }

            var recipient = usuarioRepository.findByUsernameIgnoreCase(director)
                    .or(() -> usuarioRepository.findByCorreoIgnoreCase(director))
                    .orElse(null);
            if (recipient == null) {
                continue;
            }

            if (inAppNotificationService.existsForRecipient(recipient.getId(), EVENT_CODE, projectId, dayStart)) {
                continue;
            }

            try {
                notificationPublisher.publish(new NotificationContext(
                        NotificationEventType.PROJECT_DELAYED,
                        projectId,
                        "system",
                        java.util.Map.of(
                                "projectName", project.getNombreProyecto(),
                                "overdueDeliverables", project.getEntregablesAtrasados(),
                                "recipients", List.of(director),
                                "title", "Proyecto " + projectId + " con retrasos"
                        )));
            } catch (Exception ex) {
                log.warn("[Notification] Failed to create delayed-project notification for {}: {}", projectId, ex.getMessage());
            }
        }
    }
}
