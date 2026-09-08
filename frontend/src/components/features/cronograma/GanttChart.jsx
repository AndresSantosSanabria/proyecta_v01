import React from 'react';
import { Calendar } from 'lucide-react';

const DAY_MS = 24 * 60 * 60 * 1000;
const MONTH_LABELS = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
const MONTH_FULL = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

const ZOOM_LEVELS = [
  { key: 'month', label: 'Mes', months: 1 },
  { key: 'quarter', label: 'Trimestre', months: 3 },
  { key: 'semester', label: 'Semestre', months: 6 },
  { key: 'year', label: 'Año', months: 12 },
];

const safeDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const diffDays = (from, to) => Math.round((to.getTime() - from.getTime()) / DAY_MS);

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

const toUtcMonthStart = (date) => new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), 1));

const addUtcMonths = (date, months) => new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + months, 1));

const parseYear = (value) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : new Date().getUTCFullYear();
};

const taskDates = (displayCronograma = []) => displayCronograma.flatMap((fase) => [
  safeDate(fase.fechaInicio),
  safeDate(fase.fechaFin),
  ...(fase.hitos || []).flatMap((hito) => [
    safeDate(hito.fechaInicio),
    safeDate(hito.fechaFin),
    ...(hito.entregables || []).flatMap((entregable) => [
      safeDate(entregable.fechaInicio),
      safeDate(entregable.fechaFin),
    ]),
  ]),
]).filter(Boolean);

const getZoomWindow = (focusDate, monthsCount) => {
  const start = toUtcMonthStart(focusDate);
  const end = addUtcMonths(start, monthsCount);
  return { start, end };
};

const formatZoomLabel = (focusDate, monthsCount) => {
  if (monthsCount === 1) {
    return `${MONTH_FULL[focusDate.getUTCMonth()]} ${focusDate.getUTCFullYear()}`;
  }
  if (monthsCount === 3) {
    const quarterStart = MONTH_FULL[focusDate.getUTCMonth()];
    const quarterEnd = MONTH_FULL[(focusDate.getUTCMonth() + 2) % 12];
    const year = focusDate.getUTCFullYear();
    return `${quarterStart} - ${quarterEnd} ${year}`;
  }
  if (monthsCount === 6) {
    const semStart = MONTH_FULL[focusDate.getUTCMonth()];
    const semEnd = MONTH_FULL[(focusDate.getUTCMonth() + 5) % 12];
    const year = focusDate.getUTCFullYear();
    return `${semStart} - ${semEnd} ${year}`;
  }
  return `${focusDate.getUTCFullYear()}`;
};

const buildTimeline = (displayCronograma, year, today, zoomWindow) => {
  let start;
  let end;

  if (zoomWindow) {
    start = zoomWindow.start;
    end = zoomWindow.end;
  } else {
    const dates = taskDates(displayCronograma);
    const fallbackYear = parseYear(year);
    const minYear = dates.length
      ? Math.min(...dates.map((date) => date.getUTCFullYear()))
      : fallbackYear;
    const maxYear = dates.length
      ? Math.max(...dates.map((date) => date.getUTCFullYear()))
      : fallbackYear;
    const startYear = Math.min(minYear, maxYear);
    const endYear = Math.max(minYear, maxYear);
    start = new Date(Date.UTC(startYear, 0, 1));
    end = new Date(Date.UTC(endYear + 1, 0, 1));
  }

  const monthCount = Math.max(1, ((end.getUTCFullYear() - start.getUTCFullYear()) * 12) + (end.getUTCMonth() - start.getUTCMonth()));
  const months = Array.from({ length: monthCount }, (_, index) => {
    const date = addUtcMonths(start, index);
    return {
      key: `${date.getUTCFullYear()}-${date.getUTCMonth()}`,
      label: MONTH_LABELS[date.getUTCMonth()],
      year: date.getUTCFullYear(),
    };
  });

  const yearGroups = [];
  months.forEach((month) => {
    const current = yearGroups[yearGroups.length - 1];
    if (current?.year === month.year) {
      current.span += 1;
    } else {
      yearGroups.push({ year: month.year, span: 1 });
    }
  });

  const totalDays = Math.max(1, diffDays(start, end));
  const todayPosition = today >= start && today < end
    ? (diffDays(start, today) / totalDays) * 100
    : null;
  const label = months.length === 1
    ? `${MONTH_FULL[start.getUTCMonth()]} ${start.getUTCFullYear()}`
    : start.getUTCFullYear() === end.getUTCFullYear()
      ? String(start.getUTCFullYear())
      : `${start.getUTCFullYear()} - ${end.getUTCFullYear() - (end.getUTCMonth() === 0 ? 1 : 0)}`;

  return {
    start,
    end,
    totalDays,
    monthCount,
    months,
    yearGroups,
    todayPosition,
    label,
  };
};

const formatStatus = (task, today) => {
  const progress = clamp(Number(task.avance) || 0, 0, 100);
  const end = safeDate(task.fechaFin);
  const daysToDue = end ? diffDays(today, end) : null;
  const daysOverdue = end ? diffDays(end, today) : 0;

  if (progress >= 100) {
    return {
      code: 'done',
      label: 'COMPLETADO',
      detail: 'Cerrado',
      tone: 'success',
      fillClass: 'fill-complete',
    };
  }

  if (end && daysOverdue > 0) {
    return {
      code: 'delayed',
      label: `ATRASO ${daysOverdue}d`,
      detail: 'Fuera de plazo',
      tone: 'danger',
      fillClass: 'fill-delayed',
    };
  }

  if (daysToDue !== null && daysToDue <= 8) {
    return {
      code: 'warning',
      label: `ALERTA ${Math.max(0, daysToDue)}d`,
      detail: 'Vence pronto',
      tone: 'warning',
      fillClass: 'fill-warning',
    };
  }

  return {
    code: 'ontrack',
    label: progress > 0 ? `EN CURSO ${Math.round(progress)}%` : 'EN TIEMPO',
    detail: 'Sin retraso',
    tone: 'success',
    fillClass: 'fill-progress',
  };
};

const buildTaskFrame = (task, timeline) => {
  const startDate = safeDate(task.fechaInicio);
  const endDate = safeDate(task.fechaFin) || startDate;
  let startOffsetDays;
  let endOffsetDays;

  if (startDate) {
    const clampedStartDate = new Date(Math.min(Math.max(startDate.getTime(), timeline.start.getTime()), timeline.end.getTime() - DAY_MS));
    const clampedEndDate = new Date(Math.min(Math.max((endDate || startDate).getTime(), clampedStartDate.getTime()), timeline.end.getTime() - DAY_MS));
    startOffsetDays = diffDays(timeline.start, clampedStartDate);
    endOffsetDays = diffDays(timeline.start, clampedEndDate);
  } else {
    startOffsetDays = (Number(task.mesInicio) || 0) * 30;
    endOffsetDays = startOffsetDays + (Math.max(1, Math.ceil(Number(task.duracionMeses) || 1)) * 30) - 1;
  }

  startOffsetDays = clamp(startOffsetDays, 0, timeline.totalDays - 1);
  endOffsetDays = clamp(endOffsetDays, startOffsetDays, timeline.totalDays - 1);
  const widthDays = Math.max(1, (endOffsetDays - startOffsetDays) + 1);

  return {
    startOffsetDays,
    widthPercent: (widthDays / timeline.totalDays) * 100,
    leftPercent: (startOffsetDays / timeline.totalDays) * 100,
  };
};

const GanttChart = ({ displayCronograma, year }) => {
  const today = new Date();
  const todayLabel = today.toLocaleDateString('es-CO', { day: '2-digit', month: 'short' }).toUpperCase();
  const [zoomLevel, setZoomLevel] = React.useState('year');
  const [focusDate, setFocusDate] = React.useState(() => toUtcMonthStart(today));
  const [expandedHitos, setExpandedHitos] = React.useState({});

  const zoomConfig = ZOOM_LEVELS.find((z) => z.key === zoomLevel) || ZOOM_LEVELS[3];
  const zoomWindow = getZoomWindow(focusDate, zoomConfig.months);
  const timeline = buildTimeline(displayCronograma, year, today, zoomWindow);
  const tableMinWidth = Math.max(920, 260 + (timeline.monthCount * 76));
  const zoomLabel = formatZoomLabel(focusDate, zoomConfig.months);

  const hasPrev = (() => {
    const allDates = taskDates(displayCronograma);
    if (!allDates.length) return false;
    const earliest = allDates.reduce((min, d) => (d < min ? d : min), allDates[0]);
    return zoomWindow.start > earliest;
  })();

  const hasNext = (() => {
    const allDates = taskDates(displayCronograma);
    if (!allDates.length) return false;
    const latest = allDates.reduce((max, d) => (d > max ? d : max), allDates[0]);
    return zoomWindow.end < addUtcMonths(latest, 1);
  })();

  const goToPrev = () => {
    setFocusDate((current) => addUtcMonths(current, -zoomConfig.months));
  };

  const goToNext = () => {
    setFocusDate((current) => addUtcMonths(current, zoomConfig.months));
  };

  const goToToday = () => {
    setFocusDate(toUtcMonthStart(today));
  };

  const toggleHito = (hitoId) => {
    setExpandedHitos((current) => ({
      ...current,
      [hitoId]: !current[hitoId],
    }));
  };

  return (
    <div className="visual-cronograma-section gantt-hero">
      <div className="section-header gantt-header">
        <div className="gantt-title-group">
          <h2>Cronograma Visual - {timeline.label}</h2>
          <p>
            La barra verde muestra el avance real. La barra roja marca atraso. La linea punteada indica el corte de hoy cuando esta dentro del rango.
          </p>
        </div>

        <div className="gantt-legend">
          <span className="legend-chip legend-success">En tiempo</span>
          <span className="legend-chip legend-warning">Alerta</span>
          <span className="legend-chip legend-danger">Atraso</span>
          <span className="legend-chip legend-today">Hoy {todayLabel}</span>
        </div>
      </div>

      <div className="gantt-zoom-controls">
        <div className="gantt-zoom-nav">
          <button type="button" className="gantt-nav-btn" onClick={goToPrev} disabled={!hasPrev} title="Anterior">
            &#8249;
          </button>
          <span className="gantt-zoom-date-label">{zoomLabel}</span>
          <button type="button" className="gantt-nav-btn" onClick={goToNext} disabled={!hasNext} title="Siguiente">
            &#8250;
          </button>
          <button type="button" className="gantt-today-btn" onClick={goToToday} title="Ir a hoy">
            <Calendar size={14} />
            Hoy
          </button>
        </div>
        <div className="gantt-zoom-levels">
          {ZOOM_LEVELS.map((level) => (
            <button
              key={level.key}
              type="button"
              className={`gantt-zoom-btn ${zoomLevel === level.key ? 'active' : ''}`}
              onClick={() => setZoomLevel(level.key)}
            >
              {level.label}
            </button>
          ))}
        </div>
      </div>

      <div className="gantt-container">
        <table className="gantt-table" style={{ minWidth: `${tableMinWidth}px` }}>
          <thead>
            <tr className="gantt-year-row">
              <th className="gantt-activity-header" rowSpan={2}>Actividad</th>
              {timeline.yearGroups.map((group) => (
                <th key={group.year} colSpan={group.span}>{group.year}</th>
              ))}
            </tr>
            <tr className="gantt-header-row">
              {timeline.months.map((month) => <th key={month.key}>{month.label}</th>)}
            </tr>
          </thead>
          <tbody>
            {displayCronograma.map((fase) => {
              const faseFrame = buildTaskFrame(fase, timeline);
              const faseStatus = formatStatus(fase, today);

              return (
                <React.Fragment key={fase.id}>
                  <tr className={`gantt-row gantt-phase-row status-${faseStatus.code}`}>
                    <td className="task-name-cell">
                      <div className="task-name-wrapper">
                        <span className="phase-name">{fase.nombre}</span>
                        <span className={`status-pill status-${faseStatus.tone}`}>{faseStatus.label}</span>
                      </div>
                    </td>
                    <td colSpan={timeline.monthCount}>
                      <div className="gantt-track" style={{ '--gantt-month-count': timeline.monthCount }}>
                        <div className="gantt-month-guides" aria-hidden="true">
                          {timeline.months.map((month, idx) => (
                            <span key={`${fase.id}-${month.key}-${idx}`} className="gantt-guide" />
                          ))}
                        </div>
                        {timeline.todayPosition !== null && (
                          <div className="gantt-today-line" style={{ left: `${timeline.todayPosition}%` }} aria-hidden="true" />
                        )}
                        <div
                          className={`bar-container phase-bar ${faseStatus.fillClass}`}
                          style={{
                            left: `${faseFrame.leftPercent}%`,
                            width: `${faseFrame.widthPercent}%`,
                          }}
                          title={`${fase.nombre}\nInicio: ${fase.fechaInicio || '--'}\nFin: ${fase.fechaFin || '--'}\nAvance: ${Number(fase.avance || 0).toFixed(0)}%\nEstado: ${faseStatus.label}`}
                        >
                          <div className="progress-fill" style={{ width: `${clamp(Number(fase.avance) || 0, 0, 100)}%` }} />
                          <div className="bar-content">
                            <span className="bar-label">{fase.id.toUpperCase()}</span>
                            <span className="bar-meta">{Number(fase.avance || 0).toFixed(0)}% - {faseStatus.detail}</span>
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>

                  {fase.hitos?.map((hito) => {
                    const hitoFrame = buildTaskFrame(hito, timeline);
                    const hitoStatus = formatStatus(hito, today);
                    const entregables = hito.entregables || [];
                    const isExpanded = Boolean(expandedHitos[hito.id]);

                    return (
                      <React.Fragment key={hito.id}>
                        <tr className={`gantt-row gantt-hito-row status-${hitoStatus.code} ${entregables.length ? 'is-clickable' : ''}`}>
                          <td className="task-name-cell">
                            <button
                              type="button"
                              className="milestone-toggle"
                              onClick={() => entregables.length && toggleHito(hito.id)}
                              aria-expanded={isExpanded}
                              disabled={!entregables.length}
                              title={entregables.length ? 'Ver entregables del hito' : 'Este hito no tiene entregables'}
                            >
                              <span className="milestone-caret">{entregables.length ? (isExpanded ? '-' : '+') : '.'}</span>
                              <span className="milestone-name">{hito.nombre}</span>
                              {entregables.length > 0 && (
                                <span className="milestone-count">{entregables.length}</span>
                              )}
                            </button>
                          </td>
                          <td colSpan={timeline.monthCount}>
                            <div className="gantt-track" style={{ '--gantt-month-count': timeline.monthCount }}>
                              <div className="gantt-month-guides" aria-hidden="true">
                                {timeline.months.map((month, idx) => (
                                  <span key={`${hito.id}-${month.key}-${idx}`} className="gantt-guide" />
                                ))}
                              </div>
                              {timeline.todayPosition !== null && (
                                <div className="gantt-today-line" style={{ left: `${timeline.todayPosition}%` }} aria-hidden="true" />
                              )}
                              <div
                                className={`bar-container milestone-bar ${hitoStatus.fillClass}`}
                                style={{
                                  left: `${hitoFrame.leftPercent}%`,
                                  width: `${hitoFrame.widthPercent}%`,
                                }}
                                title={`${hito.nombre}\nInicio: ${hito.fechaInicio || '--'}\nFin: ${hito.fechaFin || '--'}\nAvance: ${Number(hito.avance || 0).toFixed(0)}%\nEstado: ${hitoStatus.label}`}
                              >
                                <div className="progress-fill" style={{ width: `${clamp(Number(hito.avance) || 0, 0, 100)}%` }} />
                                <div className="bar-content">
                                  <span className="bar-label">{hito.id.toUpperCase()}</span>
                                  <span className="bar-meta">{Number(hito.avance || 0).toFixed(0)}% - {hitoStatus.detail}</span>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>

                        {isExpanded && entregables.map((entregable) => {
                          const entregableFrame = buildTaskFrame(entregable, timeline);
                          const entregableStatus = formatStatus(entregable, today);

                          return (
                            <tr key={entregable.id} className={`gantt-row gantt-entregable-row status-${entregableStatus.code}`}>
                              <td className="task-name-cell">
                                <div className="deliverable-name">
                                  <span className="deliverable-dot" aria-hidden="true" />
                                  <span>{entregable.nombre}</span>
                                  <span className="deliverable-weight">{Number(entregable.ponderacion || 0).toFixed(0)}%</span>
                                </div>
                              </td>
                              <td colSpan={timeline.monthCount}>
                                <div className="gantt-track" style={{ '--gantt-month-count': timeline.monthCount }}>
                                  <div className="gantt-month-guides" aria-hidden="true">
                                    {timeline.months.map((month, idx) => (
                                      <span key={`${entregable.id}-${month.key}-${idx}`} className="gantt-guide" />
                                    ))}
                                  </div>
                                  {timeline.todayPosition !== null && (
                                    <div className="gantt-today-line" style={{ left: `${timeline.todayPosition}%` }} aria-hidden="true" />
                                  )}
                                  <div
                                    className={`bar-container deliverable-bar ${entregableStatus.fillClass}`}
                                    style={{
                                      left: `${entregableFrame.leftPercent}%`,
                                      width: `${entregableFrame.widthPercent}%`,
                                    }}
                                    title={`${entregable.nombre}\nInicio: ${entregable.fechaInicio || '--'}\nFin: ${entregable.fechaFin || '--'}\nAvance: ${Number(entregable.avance || 0).toFixed(0)}%\nEstado: ${entregableStatus.label}`}
                                  >
                                    <div className="progress-fill" style={{ width: `${clamp(Number(entregable.avance) || 0, 0, 100)}%` }} />
                                    <div className="bar-content">
                                      <span className="bar-label">{entregable.id.toUpperCase()}</span>
                                      <span className="bar-meta">{Number(entregable.avance || 0).toFixed(0)}% - {entregableStatus.detail}</span>
                                    </div>
                                  </div>
                                </div>
                              </td>
                            </tr>
                          );
                        })}
                      </React.Fragment>
                    );
                  })}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default GanttChart;
