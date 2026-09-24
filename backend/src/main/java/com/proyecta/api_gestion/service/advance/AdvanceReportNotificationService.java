package com.proyecta.api_gestion.service.advance;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.advance.NotificationLog;
import com.proyecta.api_gestion.model.advance.AdvanceReportUpload;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.repository.advance.AdvanceReportUploadRepository;
import com.proyecta.api_gestion.repository.advance.NotificationLogRepository;
import com.proyecta.api_gestion.service.notification.NotificationContext;
import com.proyecta.api_gestion.service.notification.NotificationEventPublisherPort;
import com.proyecta.api_gestion.service.notification.NotificationEventType;
import com.proyecta.api_gestion.service.notification.ProjectNotificationRecipients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Servicio de notificaciones idempotente para informes de avance.
 * Garantiza que NUNCA se dupliquen envíos el mismo día al mismo proyecto
 * usando la constraint unique de notification_log + manejo de race conditions.
 */
@Service
public class AdvanceReportNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportNotificationService.class);
    private static final String NOTIFICATION_TYPE_PRE = "ADVANCE_REPORT_PRE_DUE";
    private static final String NOTIFICATION_TYPE_POST = "ADVANCE_REPORT_POST_DUE";
    private static final String NOTIFICATION_TYPE_OVERRIDE = "ADVANCE_REPORT_OVERRIDE";

    private final ProyectoRepository proyectoRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final AdvanceReportUploadRepository uploadRepository;
    private final NotificationEventPublisherPort notificationPublisher;
    private final AdvanceReportRuleEvaluator ruleEvaluator;

    public AdvanceReportNotificationService(
            ProyectoRepository proyectoRepository,
            NotificationLogRepository notificationLogRepository,
            AdvanceReportUploadRepository uploadRepository,
            NotificationEventPublisherPort notificationPublisher,
            AdvanceReportRuleEvaluator ruleEvaluator) {
        this.proyectoRepository = proyectoRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.uploadRepository = uploadRepository;
        this.notificationPublisher = notificationPublisher;
        this.ruleEvaluator = ruleEvaluator;
    }

    /**
     * Procesa un solo proyecto: evalúa reglas, verifica idempotencia y despacha.
     * Retorna true si se envió notificación, false si no correspondía.
     */
    @Transactional
    public boolean processProject(Proyecto proyecto, LocalDate today, String periodo) {
        // Skip if already uploaded for this period
        if (uploadRepository.existsByProjectIdAndPeriodo(proyecto.getId(), periodo)) {
            return false;
        }

        // Evaluate rules
        AdvanceReportRuleEvaluator.DecisionResult decision = ruleEvaluator.evaluate(today);
        if (decision.decision() == AdvanceReportRuleEvaluator.NotificationDecision.NONE) {
            return false;
        }

        // Resolve notification type
        String notificationType = switch (decision.decision()) {
            case PRE_DUE -> NOTIFICATION_TYPE_PRE;
            case POST_DUE -> NOTIFICATION_TYPE_POST;
            case OVERRIDE -> NOTIFICATION_TYPE_OVERRIDE;
            default -> null;
        };
        if (notificationType == null) return false;

        // Idempotency check: try to insert log, catch unique constraint violation
        if (notificationLogRepository.existsByProjectIdAndNotificationDateAndNotificationType(
                proyecto.getId(), today, notificationType)) {
            log.debug("Notificación ya enviada hoy para proyecto {} tipo {}", proyecto.getId(), notificationType);
            return false;
        }

        try {
            notificationLogRepository.save(
                    new NotificationLog(proyecto.getId(), today, notificationType));
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread inserted first. This is expected behavior.
            log.debug("Race condition al insertar notification_log para {} — ya fue procesado", proyecto.getId());
            return false;
        }

        // Dispatch notification
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        String projectName = proyecto.getNombre() != null ? proyecto.getNombre() : proyecto.getId();

        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ADVANCE_REPORT_DUE_NOTIFICATION,
                proyecto.getId(),
                "system",
                Map.of(
                        "projectName", projectName,
                        "periodo", periodo != null ? periodo : "",
                        "dueDate", ruleEvaluator.getDueDate() != null ? ruleEvaluator.getDueDate().toString() : "N/A",
                        "message", decision.message() != null ? decision.message() : "",
                        "recipients", recipients,
                        "title", "Informe de avance pendiente — " + proyecto.getId()
                )));

        log.info("Notificación de informe de avance enviada para proyecto {} tipo {}", proyecto.getId(), notificationType);
        return true;
    }

    /**
     * Retorna true si un proyecto tiene informe cargado para el periodo dado.
     */
    @Transactional(readOnly = true)
    public boolean isUploaded(String projectId, String periodo) {
        return uploadRepository.existsByProjectIdAndPeriodo(projectId, periodo);
    }

    /**
     * Retorna el upload de un proyecto para un periodo.
     */
    @Transactional(readOnly = true)
    public AdvanceReportUpload getUpload(String projectId, String periodo) {
        return uploadRepository.findByProjectIdAndPeriodo(projectId, periodo).orElse(null);
    }

    @Transactional
    public void notificarInformeCargado(Proyecto proyecto, String periodo, String fileName, String actorUsername) {
        if (proyecto == null) return;
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) return;
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ADVANCE_REPORT_UPLOADED,
                proyecto.getId(),
                actorUsername == null || actorUsername.isBlank() ? "system" : actorUsername,
                Map.of(
                        "projectName", nombreProyecto(proyecto),
                        "periodo", periodo != null ? periodo : "",
                        "fileName", fileName != null ? fileName : "",
                        "recipients", recipients
                )));
    }

    @Transactional
    public void notificarInformeVerificado(Proyecto proyecto, String periodo, String actorUsername) {
        if (proyecto == null) return;
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) return;
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ADVANCE_REPORT_VERIFIED,
                proyecto.getId(),
                actorUsername == null || actorUsername.isBlank() ? "system" : actorUsername,
                Map.of(
                        "projectName", nombreProyecto(proyecto),
                        "periodo", periodo != null ? periodo : "",
                        "recipients", recipients
                )));
    }

    @Transactional
    public void notificarInformeDevuelto(Proyecto proyecto, String periodo, String observaciones, String actorUsername) {
        if (proyecto == null) return;
        List<String> recipients = ProjectNotificationRecipients.resolve(proyecto);
        if (recipients.isEmpty()) return;
        notificationPublisher.publish(new NotificationContext(
                NotificationEventType.ADVANCE_REPORT_RETURNED,
                proyecto.getId(),
                actorUsername == null || actorUsername.isBlank() ? "system" : actorUsername,
                Map.of(
                        "projectName", nombreProyecto(proyecto),
                        "periodo", periodo != null ? periodo : "",
                        "observaciones", observaciones != null && !observaciones.isBlank() ? observaciones : "Sin observaciones",
                        "recipients", recipients
                )));
    }

    private String nombreProyecto(Proyecto proyecto) {
        return proyecto.getNombre() != null ? proyecto.getNombre() : proyecto.getId();
    }
}
