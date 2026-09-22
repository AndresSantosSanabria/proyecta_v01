package com.proyecta.api_gestion.service.advance;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Job diario que escanea proyectos activos con upload_status=false
 * y aplica las reglas de ventana/intervalo para notificar.
 */
@Service
public class AdvanceReportReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportReminderScheduler.class);

    private final ProyectoRepository proyectoRepository;
    private final AdvanceReportNotificationService notificationService;
    private final AdvanceReportRuleEvaluator ruleEvaluator;

    public AdvanceReportReminderScheduler(
            ProyectoRepository proyectoRepository,
            AdvanceReportNotificationService notificationService,
            AdvanceReportRuleEvaluator ruleEvaluator) {
        this.proyectoRepository = proyectoRepository;
        this.notificationService = notificationService;
        this.ruleEvaluator = ruleEvaluator;
    }

    @Scheduled(cron = "${notifications.advance-report.cron:0 0 7 * * *}")
    @Transactional
    public void scanAndNotifyPendingReports() {
        LocalDate today = LocalDate.now();
        String periodo = resolveCurrentPeriodo(today);

        log.info("▶ Evaluando informes de avance para periodo {}", periodo);

        // Get all active projects (not closed, not finalized)
        var proyectos = proyectoRepository.findAll().stream()
                .filter(p -> !isTerminal(p))
                .toList();

        int notified = 0;
        for (Proyecto proyecto : proyectos) {
            try {
                if (notificationService.processProject(proyecto, today, periodo)) {
                    notified++;
                }
            } catch (Exception e) {
                log.error("Error procesando informe de avance para proyecto {}: {}",
                        proyecto.getId(), e.getMessage());
            }
        }

        log.info("✓ Evaluación de informes completada: {}/{} proyectos notificados", notified, proyectos.size());
    }

    private boolean isTerminal(Proyecto proyecto) {
        if (proyecto.getEstadoConfig() != null) {
            return proyecto.getEstadoConfig().getEsTerminal();
        }
        return EstadoProyecto.CERRADO.equals(proyecto.getEstado())
                || EstadoProyecto.CERRADO_FORZOSO.equals(proyecto.getEstado())
                || EstadoProyecto.FINALIZADO.equals(proyecto.getEstado());
    }

    /**
     * Genera el código de periodo basado en la fecha.
     * Formato: YYYY-QN (ej: 2026-Q3) basado en trimestre.
     */
    private String resolveCurrentPeriodo(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return String.format("%d-Q%d", date.getYear(), quarter);
    }
}
