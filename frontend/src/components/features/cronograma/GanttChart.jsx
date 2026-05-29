import React from 'react';

const DAY_MS = 24 * 60 * 60 * 1000;
const MONTH_COUNT = 12;

const safeDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

const diffDays = (from, to) => Math.round((to.getTime() - from.getTime()) / DAY_MS);

const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

const monthIndex = (date) => {
  if (!date) return 0;
  return typeof date.getUTCMonth === 'function' ? date.getUTCMonth() : date.getMonth();
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

const buildTaskFrame = (startMonthIndex, endMonthIndex, today) => {
  const startMonth = clamp(Number(startMonthIndex) || 0, 0, MONTH_COUNT - 1);
  const endMonth = clamp(Number(endMonthIndex) || startMonth, startMonth, MONTH_COUNT - 1);
  const widthMonths = Math.max(1, (endMonth - startMonth) + 1);
  const todayMonth = clamp(monthIndex(today), 0, MONTH_COUNT - 1);
  const todayPosition = ((todayMonth + 0.5) / MONTH_COUNT) * 100;

  return {
    startMonth,
    widthPercent: (widthMonths / MONTH_COUNT) * 100,
    leftPercent: (startMonth / MONTH_COUNT) * 100,
    todayPosition,
  };
};

const GanttChart = ({ displayCronograma, meses, year }) => {
  const today = new Date();
  const todayLabel = today.toLocaleDateString('es-CO', { day: '2-digit', month: 'short' }).toUpperCase();

  return (
    <div className="visual-cronograma-section gantt-hero">
      <div className="section-header gantt-header">
        <div className="gantt-title-group">
          <h2>Cronograma Visual — {year}</h2>
          <p>
            La barra verde muestra el avance real. La barra roja marca atraso. La línea punteada indica el corte de hoy.
          </p>
        </div>

        <div className="gantt-legend">
          <span className="legend-chip legend-success">En tiempo</span>
          <span className="legend-chip legend-warning">Alerta</span>
          <span className="legend-chip legend-danger">Atraso</span>
          <span className="legend-chip legend-today">Hoy {todayLabel}</span>
        </div>
      </div>

      <div className="gantt-container">
        <table className="gantt-table">
          <thead>
            <tr className="gantt-header-row">
              <th>Actividad</th>
              {meses.map((mes) => <th key={mes}>{mes}</th>)}
            </tr>
          </thead>
          <tbody>
            {displayCronograma.map((fase) => {
              const faseFrame = buildTaskFrame(fase.mesInicio, fase.mesInicio + Math.max(1, Math.ceil(fase.duracionMeses)) - 1, today);
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
                    <td colSpan={12}>
                      <div className="gantt-track">
                        <div className="gantt-month-guides" aria-hidden="true">
                          {meses.map((mes, idx) => (
                            <span key={`${fase.id}-${mes}-${idx}`} className="gantt-guide" />
                          ))}
                        </div>
                        <div className="gantt-today-line" style={{ left: `${faseFrame.todayPosition}%` }} aria-hidden="true" />
                        <div
                          className={`bar-container phase-bar ${faseStatus.fillClass}`}
                          style={{
                            left: `${faseFrame.leftPercent}%`,
                            width: `${faseFrame.widthPercent}%`,
                          }}
                          title={`${fase.nombre}\nAvance: ${Number(fase.avance || 0).toFixed(0)}%\nEstado: ${faseStatus.label}`}
                        >
                          <div className="progress-fill" style={{ width: `${clamp(Number(fase.avance) || 0, 0, 100)}%` }} />
                          <div className="bar-content">
                            <span className="bar-label">{fase.id.toUpperCase()}</span>
                            <span className="bar-meta">{Number(fase.avance || 0).toFixed(0)}% · {faseStatus.detail}</span>
                          </div>
                        </div>
                      </div>
                    </td>
                  </tr>

                  {fase.hitos?.map((hito) => {
                    const hitoFrame = buildTaskFrame(hito.mesInicio, hito.mesInicio + Math.max(1, Math.ceil(hito.duracionMeses)) - 1, today);
                    const hitoStatus = formatStatus(hito, today);

                    return (
                      <tr key={hito.id} className={`gantt-row gantt-hito-row status-${hitoStatus.code}`}>
                        <td className="task-name-cell">
                          <span className="milestone-name">{hito.nombre}</span>
                        </td>
                        <td colSpan={12}>
                          <div className="gantt-track">
                            <div className="gantt-month-guides" aria-hidden="true">
                              {meses.map((mes, idx) => (
                                <span key={`${hito.id}-${mes}-${idx}`} className="gantt-guide" />
                              ))}
                            </div>
                            <div className="gantt-today-line" style={{ left: `${hitoFrame.todayPosition}%` }} aria-hidden="true" />
                            <div
                              className={`bar-container milestone-bar ${hitoStatus.fillClass}`}
                              style={{
                                left: `${hitoFrame.leftPercent}%`,
                                width: `${hitoFrame.widthPercent}%`,
                              }}
                              title={`${hito.nombre}\nInicio: ${hito.fechaInicio}\nFin: ${hito.fechaFin}\nAvance: ${Number(hito.avance || 0).toFixed(0)}%\nEstado: ${hitoStatus.label}`}
                            >
                              <div className="progress-fill" style={{ width: `${clamp(Number(hito.avance) || 0, 0, 100)}%` }} />
                              <div className="bar-content">
                                <span className="bar-label">{hito.id.toUpperCase()}</span>
                                <span className="bar-meta">{Number(hito.avance || 0).toFixed(0)}% · {hitoStatus.detail}</span>
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
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default GanttChart;
