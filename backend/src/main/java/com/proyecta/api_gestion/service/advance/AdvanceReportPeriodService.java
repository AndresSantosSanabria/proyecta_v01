package com.proyecta.api_gestion.service.advance;

import com.proyecta.api_gestion.model.Proyecto;
import com.proyecta.api_gestion.model.enums.EstadoProyecto;
import com.proyecta.api_gestion.model.enums.ViabilidadEstado;
import com.proyecta.api_gestion.service.config.SystemParameterKeys;
import com.proyecta.api_gestion.service.config.SystemParameterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Calcula periodo, fecha limite y elegibilidad del informe de avance.
 * Todas las reglas son parametrizables desde Configuracion (SystemParameterService):
 *  - advance_report_period_months        cadencia de solicitud (3 = trimestres de calendario)
 *  - advance_report_due_day              dia limite dentro del periodo (0 = ultimo dia)
 *  - advance_report_min_project_age_months meses minimos desde fecha_inicio
 *  - advance_report_eligible_states      estados del proyecto que reciben solicitud
 *  - advance_report_enabled              habilitacion general
 *  - advance_report_due_date             override manual de fecha limite
 */
@Service
public class AdvanceReportPeriodService {

    private static final Logger log = LoggerFactory.getLogger(AdvanceReportPeriodService.class);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> DEFAULT_ELIGIBLE_STATES = Set.of("ACTIVO", "CON_RETRASOS", "EN_REVISION");

    private final SystemParameterService systemParameterService;

    public AdvanceReportPeriodService(SystemParameterService systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(
                systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_ENABLED, "true"));
    }

    public int getPeriodMonths() {
        int months = systemParameterService.getInt(SystemParameterKeys.ADVANCE_REPORT_PERIOD_MONTHS, 3);
        if (months != 1 && months != 3 && months != 6 && months != 12) {
            log.warn("advance_report_period_months invalido: {} — se usa 3", months);
            return 3;
        }
        return months;
    }

    public int getMinProjectAgeMonths() {
        int months = systemParameterService.getInt(SystemParameterKeys.ADVANCE_REPORT_MIN_PROJECT_AGE_MONTHS, 3);
        if (months < 0) {
            log.warn("advance_report_min_project_age_months invalido: {} — se usa 3", months);
            return 3;
        }
        return months;
    }

    public int getDueDay() {
        int day = systemParameterService.getInt(SystemParameterKeys.ADVANCE_REPORT_DUE_DAY, 0);
        if (day < 0 || day > 31) {
            log.warn("advance_report_due_day invalido: {} — se usa 0 (ultimo dia)", day);
            return 0;
        }
        return day;
    }

    /**
     * Codigo de periodo actual. Formato segun cadencia:
     *  3 meses -> YYYY-QN (compatibilidad con datos existentes), 1 -> YYYY-MNN,
     *  6 -> YYYY-SN, 12 -> YYYY.
     */
    public String currentPeriodo(LocalDate date) {
        int periodMonths = getPeriodMonths();
        switch (periodMonths) {
            case 1:
                return String.format("%d-M%02d", date.getYear(), date.getMonthValue());
            case 6: {
                int semester = date.getMonthValue() <= 6 ? 1 : 2;
                return String.format("%d-S%d", date.getYear(), semester);
            }
            case 12:
                return String.valueOf(date.getYear());
            default: {
                int quarter = (date.getMonthValue() - 1) / 3 + 1;
                return String.format("%d-Q%d", date.getYear(), quarter);
            }
        }
    }

    /**
     * Primer y ultimo dia del periodo que contiene la fecha dada.
     */
    public LocalDate startOfPeriod(LocalDate date) {
        int periodMonths = getPeriodMonths();
        if (periodMonths == 12) {
            return LocalDate.of(date.getYear(), 1, 1);
        }
        int monthIndex = ((date.getMonthValue() - 1) / periodMonths) * periodMonths;
        return LocalDate.of(date.getYear(), monthIndex + 1, 1);
    }

    public LocalDate endOfPeriod(LocalDate date) {
        int periodMonths = getPeriodMonths();
        if (periodMonths == 12) {
            return LocalDate.of(date.getYear(), 12, 31);
        }
        int monthIndex = ((date.getMonthValue() - 1) / periodMonths) * periodMonths + periodMonths;
        int year = date.getYear();
        if (monthIndex > 12) {
            monthIndex -= 12;
            year++;
        }
        return LocalDate.of(year, monthIndex, 1).plusMonths(1).minusDays(1);
    }

    /**
     * Fecha limite del periodo vigente.
     * 1) advance_report_due_date solo aplica si cae dentro del periodo vigente (override manual).
     * 2) advance_report_due_day (>0) = ese dia del ultimo mes del periodo.
     * 3) Default = ultimo dia del periodo.
     */
    public LocalDate dueDateForPeriodo(LocalDate today) {
        LocalDate start = startOfPeriod(today);
        LocalDate end = endOfPeriod(today);

        String rawOverride = systemParameterService.getString(SystemParameterKeys.ADVANCE_REPORT_DUE_DATE, "");
        if (rawOverride != null && !rawOverride.isBlank()) {
            try {
                LocalDate override = LocalDate.parse(rawOverride.trim(), ISO_DATE);
                if (!override.isBefore(start) && !override.isAfter(end)) {
                    return override;
                }
            } catch (Exception e) {
                log.warn("Valor invalido para advance_report_due_date: '{}'", rawOverride);
            }
        }

        int dueDay = getDueDay();
        if (dueDay > 0) {
            LocalDate candidate = LocalDate.of(end.getYear(), end.getMonthValue(), 1).plusDays(dueDay - 1);
            if (!candidate.isAfter(end)) {
                return candidate;
            }
        }
        return end;
    }

    /**
     * Regla central de elegibilidad: el proyecto recibe solicitud de informe de avance.
     * Todos los filtros son parametrizables; se aplica igual en el scheduler y en GET /pending.
     */
    public boolean esElegible(Proyecto proyecto, LocalDate today) {
        if (proyecto == null) {
            return false;
        }
        if (!isEnabled()) {
            return false;
        }
        if (!esEstadoElegible(proyecto)) {
            return false;
        }
        if (!proyecto.documentosPreWizardCompletos()
                || !ViabilidadEstado.APROBADA.equals(proyecto.getViabilidadEstado())) {
            return false;
        }
        LocalDate fechaInicio = proyecto.getFechaInicio();
        if (fechaInicio == null) {
            return false;
        }
        int minAgeMonths = getMinProjectAgeMonths();
        return !fechaInicio.isAfter(today.minusMonths(minAgeMonths));
    }

    private boolean esEstadoElegible(Proyecto proyecto) {
        String raw = systemParameterService.getString(
                SystemParameterKeys.ADVANCE_REPORT_ELIGIBLE_STATES,
                String.join(",", DEFAULT_ELIGIBLE_STATES));
        Set<String> estados = new LinkedHashSet<>();
        if (raw != null && !raw.isBlank()) {
            Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.toUpperCase().replace('-', '_').replace(' ', '_'))
                    .forEach(estados::add);
        }
        if (estados.isEmpty()) {
            estados.addAll(DEFAULT_ELIGIBLE_STATES);
        }

        String codigo = proyecto.getEstadoCodigo();
        if (codigo == null && proyecto.getEstado() != null) {
            codigo = proyecto.getEstado().name();
        }
        if (codigo == null) {
            return false;
        }
        if (estados.contains(codigo.toUpperCase())) {
            return !proyecto.esEstadoTerminal();
        }
        return false;
    }
}
