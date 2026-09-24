package com.proyecta.api_gestion.service.advance;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.repository.ProyectoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Job diario que escanea proyectos elegibles (estado iniciado, viabilidad aprobada,
 * antiguedad minima configurable) sin informe del periodo vigente y aplica las
 * reglas de ventana/intervalo para notificar. Todas las reglas son parametrizables
 * desde Configuracion via AdvanceReportPeriodService.
 */
@Service
public class AdvanceReportReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportReminderScheduler.class);

    private final ProyectoRepository proyectoRepository;
    private final AdvanceReportNotificationService notificationService;
    private final AdvanceReportRuleEvaluator ruleEvaluator;
    private final AdvanceReportPeriodService periodService;

    public AdvanceReportReminderScheduler(
            ProyectoRepository proyectoRepository,
            AdvanceReportNotificationService notificationService,
            AdvanceReportRuleEvaluator ruleEvaluator,
            AdvanceReportPeriodService periodService) {
        this.proyectoRepository = proyectoRepository;
        this.notificationService = notificationService;
        this.ruleEvaluator = ruleEvaluator;
        this.periodService = periodService;
    }

    @Scheduled(cron = "${notifications.advance-report.cron:0 0 7 * * *}")
    @Transactional
    public void scanAndNotifyPendingReports() {
        LocalDate today = LocalDate.now();
        String periodo = periodService.currentPeriodo(today);

        if (!periodService.isEnabled()) {
            log.info("Informe de avance deshabilitado (advance_report_enabled=false), omitiendo escaneo");
            return;
        }

        log.info("▶ Evaluando informes de avance para periodo {}", periodo);

        var proyectos = proyectoRepository.findAll().stream()
                .filter(p -> periodService.esElegible(p, today))
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

        log.info("✓ Evaluación de informes completada: {}/{} proyectos elegibles notificados",
                notified, proyectos.size());
    }
}
