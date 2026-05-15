import React from 'react';
import { 
  Lock, 
  FileText, 
  Upload, 
  Check, 
  ChevronDown, 
  ChevronRight, 
  Layers, 
  Target, 
  Package 
} from 'lucide-react';

const TreeTableRow = ({ 
  fase, 
  isExpanded, 
  toggleNode 
}) => {
  return (
    <React.Fragment>
      <tr 
        className="row-phase clickable-row" 
        onClick={() => toggleNode(`fase-${fase.id}`)}
      >
        <td className="col-hito indent-1">
          <div className="node-title">
            {isExpanded(`fase-${fase.id}`) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
            <span className="node-badge fase-badge"><Layers size={12} /> FASE</span>
            <strong>{fase.nombre}</strong>
          </div>
        </td>
        <td>{fase.ponderacion}%</td>
        <td>—</td>
        <td>
          <div className="progress-cell">
            <div className="progress-bar-bg small">
              <div className={`progress-bar-fill ${fase.avance === 100 ? 'success-bg' : ''}`} style={{ width: `${fase.avance}%` }}></div>
            </div>
            <span>{fase.avance}% <Lock size={10} className="text-muted"/></span>
          </div>
        </td>
        <td>—</td>
        <td>—</td>
      </tr>

      {isExpanded(`fase-${fase.id}`) && fase.hitos.map((hito) => (
        <React.Fragment key={hito.id}>
          <tr 
            className="row-milestone clickable-row"
            onClick={() => toggleNode(`hito-${hito.id}`)}
          >
            <td className="col-hito indent-2">
              <div className="node-title">
                {isExpanded(`hito-${hito.id}`) ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                <span className="node-badge hito-badge"><Target size={12} /> HITO</span>
                <span>{hito.nombre}</span>
              </div>
            </td>
            <td>{hito.ponderacion}%</td>
            <td>—</td>
            <td>
              <div className="progress-cell">
                <div className="progress-bar-bg small">
                  <div className={`progress-bar-fill ${hito.avance === 100 ? 'success-bg' : ''}`} style={{ width: `${hito.avance}%` }}></div>
                </div>
                <span>{hito.avance}% <Lock size={10} className="text-muted"/></span>
              </div>
            </td>
            <td>—</td>
            <td>—</td>
          </tr>

          {isExpanded(`hito-${hito.id}`) && hito.entregables.map((ent) => (
            <tr key={ent.id} className="row-deliverable">
              <td className="col-hito indent-3">
                <div className="deliverable-content">
                  <div className="node-title">
                    <span className="node-badge entregable-badge"><Package size={12} /> ENTREGABLE</span>
                    <span>{ent.nombre}</span>
                  </div>
                  {ent.archivo && (
                    <a href="#" className="file-link"><FileText size={14} /> {ent.archivo}</a>
                  )}
                </div>
              </td>
              <td>{ent.ponderacion}%</td>
              <td>{ent.fechaLimite}</td>
              <td>
                {ent.estado === 'A conformidad' ? (
                  <span className="status-badge success"><Check size={12} /> A conformidad</span>
                ) : (
                  <div className="progress-cell">
                    <span>{ent.avance}% <Lock size={10} className="text-muted"/></span>
                  </div>
                )}
              </td>
              <td>
                 {ent.estado === 'Atrasado' ? (
                   <span className="status-badge danger">Atrasado</span>
                 ) : ent.estado === 'A conformidad' ? (
                   <span className="text-success small">✓ Entregado {ent.fechaEntrega}</span>
                 ) : '—'}
              </td>
              <td className={ent.atraso ? 'text-danger fw-bold' : ''}>
                 {ent.atraso || '—'}
              </td>
              {ent.estado !== 'A conformidad' && (
                 <td className="action-col-absolute">
                    <button className="btn-upload"><Upload size={14} /> Marcar OK + subir PDF</button>
                 </td>
              )}
            </tr>
          ))}
        </React.Fragment>
      ))}
    </React.Fragment>
  );
};

const ProgressTreeTable = ({ progressData, isExpanded, toggleNode }) => {
  return (
    <div className="detailed-table-container">
      <div className="table-header">
        <h2>Avance Detallado — {progressData.nombre}</h2>
        <div className="header-actions">
          <span className="corte-date">Corte: 8/4/2026</span>
          <button className="btn-report"><FileText size={16} /> Generar Reporte</button>
        </div>
      </div>

      <div className="tree-table-wrapper">
        <table className="tree-table">
          <thead>
            <tr>
              <th className="col-hito">HITO / ENTREGABLE</th>
              <th className="col-ponderacion">PONDERACIÓN <Lock size={12} /></th>
              <th className="col-fecha">FECHA LÍMITE</th>
              <th className="col-avance">AVANCE <Lock size={12} /></th>
              <th className="col-estado">ESTADO <Lock size={12} /></th>
              <th className="col-atraso">ATRASO <Lock size={12} /></th>
            </tr>
          </thead>
          <tbody>
            {/* FILA PROYECTO RAIZ */}
            <tr className="row-root">
              <td className="col-hito"><strong>▼ {progressData.codigo} — {progressData.nombre}</strong></td>
              <td><strong>100%</strong></td>
              <td>—</td>
              <td>
                <div className="progress-cell">
                  <div className="progress-bar-bg small">
                    <div className="progress-bar-fill" style={{ width: `${progressData.avanceTotal}%` }}></div>
                  </div>
                  <span>{progressData.avanceTotal}%</span>
                </div>
              </td>
              <td>—</td>
              <td>—</td>
            </tr>

            {/* RENDERIZADO RECURSIVO DE FASES E HITOS */}
            {progressData.fases.map((fase) => (
              <TreeTableRow 
                key={fase.id} 
                fase={fase} 
                isExpanded={isExpanded} 
                toggleNode={toggleNode} 
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ProgressTreeTable;
