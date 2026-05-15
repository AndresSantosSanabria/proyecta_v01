import React from 'react';

const GanttChart = ({ displayCronograma, meses }) => {
  return (
    <div className="visual-cronograma-section">
      <div className="section-header">
        <h2>Cronograma Visual — 2026</h2>
      </div>
      <div className="gantt-container">
        <table className="gantt-table">
          <thead>
            <tr className="gantt-header-row">
              <th></th>
              {meses.map(mes => <th key={mes}>{mes}</th>)}
            </tr>
          </thead>
          <tbody>
            {displayCronograma.map(fase => (
              <React.Fragment key={fase.id}>
                {/* Fase Row */}
                <tr className="gantt-row">
                  <td className="task-name-cell">
                    <div className="task-name-wrapper">
                      <span className="phase-name">{fase.nombre}</span>
                    </div>
                  </td>
                  <td colSpan={12}>
                    <div 
                      className="bar-container phase-bar" 
                      style={{ 
                        left: `${(fase.mesInicio / 12) * 100}%`, 
                        width: `${(fase.duracionMeses / 12) * 100}%` 
                      }}
                      title={`${fase.nombre}\nAvance: ${fase.avance.toFixed(0)}%`}
                    >
                      <div className="progress-fill" style={{ width: `${fase.avance}%` }}></div>
                      <span className="bar-label">{fase.id.toUpperCase()}</span>
                    </div>
                  </td>
                </tr>
                {/* Hitos Rows */}
                {fase.hitos?.map(hito => (
                  <tr key={hito.id} className="gantt-row">
                    <td className="task-name-cell">
                      <span className="milestone-name">{hito.nombre}</span>
                    </td>
                    <td colSpan={12}>
                      <div 
                        className="bar-container milestone-bar" 
                        style={{ 
                          left: `${(hito.mesInicio / 12) * 100}%`, 
                          width: `${(hito.duracionMeses / 12) * 100}%` 
                        }}
                        title={`${hito.nombre}\nInicio: ${hito.fechaInicio}\nFin: ${hito.fechaFin}\nAvance: ${hito.avance.toFixed(0)}%`}
                      >
                        <div className="progress-fill" style={{ width: `${hito.avance}%` }}></div>
                        <span className="bar-label">{hito.id.toUpperCase()}</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default GanttChart;
