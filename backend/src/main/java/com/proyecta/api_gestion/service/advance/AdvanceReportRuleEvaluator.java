package com.proyecta.api_gestion.service.advance;

import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de reglas dinámico para evaluación de ventanas e intervalos.
 * Calcula si el día actual cumple las reglas de notificación para un proyecto.
 */
@Service
public class AdvanceReportRuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportRuleEvaluator.class);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SystemParameterService systemParameterService;

    public AdvanceReportRuleEvaluator(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    /**
     * Resultado de evaluación: qué tipo de notificación corresponde (si alguna).
     */
    public enum NotificationDecision {
        NONE,
        PRE_DUE,
        POST_DUE,
        OVERRIDE
    }

    public record DecisionResult(NotificationDecision decision, String message) {
        public static DecisionResult none() {
            return new DecisionResult(NotificationDecision.NONE, null);
        }
        public static DecisionResult preDue(String msg) {
            return new DecisionResult(NotificationDecision.PRE_DUE, msg);
        }
        public static DecisionResult postDue(String msg) {
            return new DecisionResult(NotificationDecision.POST_DUE, msg);
        }
        public static DecisionResult override(String msg) {
            return new DecisionResult(NotificationDecision.OVERRIDE, msg);
        }
    }

    /**
     * Evalúa si hoy se debe notificar para un proyecto dado.
     * Retorna el tipo de notificación y un mensaje descriptivo, o NONE si no corresponde.
     */
    public DecisionResult evaluate(LocalDate today) {
        LocalDate dueDate = resolveDueDate();
        if (dueDate == null) {
            log.debug("No hay advance_report_due_date configurado, saltando evaluación");
            return DecisionResult.none();
        }

        // 1. Check override dates first
        List<LocalDate> overrideDates = resolveOverrideDates();
        if (overrideDates.contains(today)) {
            return DecisionResult.override("Fecha de disparo manual programada.");
        }

        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);

        // 2. Pre-due window check
        int preDueWindow = systemParameterService.getInt(
                SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_WINDOW_DAYS, 15);
        int preDueInterval = systemParameterService.getInt(
                SystemParameterKeys.ADVANCE_REPORT_PRE_DUE_INTERVAL_DAYS, 3);

        if (daysUntilDue >= 0 && daysUntilDue <= preDueWindow) {
            // Within pre-due window: check if today falls on an interval day
            // Day 0 = due date, Day 1 = 1 day before, etc.
            long daysSinceWindowStart = preDueWindow - daysUntilDue;
            if (daysSinceWindowStart % preDueInterval == 0) {
                String msg = String.format(
                        "Faltan %d día(s) para la fecha límite (%s). "
                        + "Recuerde cargar el informe de avance.",
                        daysUntilDue, dueDate.format(ISO_DATE));
                return DecisionResult.preDue(msg);
            }
        }

        // 3. Post-due interval check
        if (daysUntilDue < 0) {
            long daysOverdue = Math.abs(daysUntilDue);
            int postDueInterval = systemParameterService.getInt(
                    SystemParameterKeys.ADVANCE_REPORT_POST_DUE_INTERVAL_DAYS, 7);

            if (daysOverdue % postDueInterval == 0) {
                String msg = String.format(
                        "El informe de avance venció hace %d día(s) (fecha límite: %s). "
                        + "Por favor cargue el documento lo antes posible.",
                        daysOverdue, dueDate.format(ISO_DATE));
                return DecisionResult.postDue(msg);
            }
        }

        return DecisionResult.none();
    }

    /**
     * Retorna la fecha límite configurada, o null si no está configurada.
     */
    public LocalDate getDueDate() {
        return resolveDueDate();
    }

    /**
     * Retorna true si el periodo actual está vencido (today > due_date).
     */
    public boolean isOverdue(LocalDate today) {
        LocalDate dueDate = resolveDueDate();
        return dueDate != null && today.isAfter(dueDate);
    }

    /**
     * Retorna los días restantes hasta la fecha límite (negativo si vencido).
     */
    public long getDaysUntilDue(LocalDate today) {
        LocalDate dueDate = resolveDueDate();
        if (dueDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(today, dueDate);
    }

    private LocalDate resolveDueDate() {
        String raw = systemParameterService.getString(
                SystemParameterKeys.ADVANCE_REPORT_DUE_DATE, "");
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim(), ISO_DATE);
        } catch (Exception e) {
            log.warn("Valor inválido para advance_report_due_date: '{}'", raw);
            return null;
        }
    }

    private List<LocalDate> resolveOverrideDates() {
        String raw = systemParameterService.getString(
                SystemParameterKeys.ADVANCE_REPORT_SPECIFIC_OVERRIDE_DATES, "");
        List<LocalDate> dates = new ArrayList<>();
        if (raw == null || raw.isBlank()) return dates;
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("[")) cleaned = cleaned.substring(1);
            if (cleaned.endsWith("]")) cleaned = cleaned.substring(0, cleaned.length() - 1);
            if (cleaned.isBlank()) return dates;
            for (String part : cleaned.split(",")) {
                String token = part.trim().replace("\"", "").replace("'", "");
                if (!token.isBlank()) {
                    dates.add(LocalDate.parse(token, ISO_DATE));
                }
            }
        } catch (Exception e) {
            log.warn("Valor inválido para advance_report_specific_override_dates: '{}'", raw);
        }
        return dates;
    }
}
