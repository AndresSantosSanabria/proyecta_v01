import React from 'react';
import { Lock, TrendingUp, TrendingDown, Percent, CircleDot } from 'lucide-react';

const toNumber = (value) => {
  if (value == null) return 0;
  const parsed = typeof value === 'string' ? parseFloat(value) : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const ProgressKPIs = ({ progressData }) => {
  const programado = toNumber(progressData.progresoProgramado ?? progressData.avanceProgramado ?? 0);
  const ejecutado = toNumber(progressData.progresoEjecutado ?? progressData.avanceTotal ?? 0);
  const diferencia = toNumber(progressData.diferencia ?? (programado - ejecutado));
  const eficacia = toNumber(progressData.eficacia ?? (programado === 0 ? 1 : ejecutado / programado));

  const completionRatio = programado > 0 ? Math.min(100, (ejecutado / programado) * 100) : 100;
  const atrasoRatio = progressData.entregablesTotal ? (toNumber(progressData.entregablesAtrasados) / toNumber(progressData.entregablesTotal)) * 100 : 0;

  return (
    <div className="progress-kpi-grid compact-grid">
      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">PROGRAMADO</h3>
          <div className="kpi-value warning">{programado.toFixed(2)}%</div>
          <p className="kpi-detail lock-detail"><Lock size={12} /> calculado por árbol</p>
        </div>
        <div className="kpi-progress-container">
          <div className="progress-bar-fill" style={{ width: `${programado}%` }} />
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">EJECUTADO</h3>
          <div className="kpi-value success">{ejecutado.toFixed(2)}%</div>
          <p className="kpi-detail lock-detail"><TrendingUp size={12} /> avance real consolidado</p>
        </div>
        <div className="kpi-progress-container">
          <div className="progress-bar-fill success-bg" style={{ width: `${completionRatio}%` }} />
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">DIFERENCIA</h3>
          <div className={`kpi-value ${diferencia <= 0 ? 'success' : 'danger'}`}>{diferencia.toFixed(2)}%</div>
          <p className="kpi-detail lock-detail"><TrendingDown size={12} /> programado - ejecutado</p>
        </div>
        <div className="kpi-progress-container">
          <div className={`progress-bar-fill ${diferencia <= 0 ? 'success-bg' : 'danger-bg'}`} style={{ width: `${Math.min(100, Math.abs(diferencia))}%` }} />
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">EFICACIA</h3>
          <div className="kpi-value success">{(eficacia * 100).toFixed(1)}%</div>
          <p className="kpi-detail lock-detail"><Percent size={12} /> ratio de ejecución</p>
        </div>
        <div className="kpi-progress-container">
          <div className="progress-bar-fill success-bg" style={{ width: `${Math.min(100, eficacia * 100)}%` }} />
        </div>
      </div>

      <div className="kpi-card progress-card meta-card">
        <div className="kpi-content">
          <h3 className="kpi-title">ESTADO</h3>
          <div className={`kpi-value ${progressData.estado === 'ATRASO' ? 'danger' : 'success'}`}>{progressData.estado || 'EN_TIEMPO'}</div>
          <p className="kpi-detail lock-detail"><CircleDot size={12} /> {toNumber(progressData.entregablesAtrasados)} entregables atrasados</p>
        </div>
        <div className="kpi-progress-container">
          <div className={`progress-bar-fill ${progressData.estado === 'ATRASO' ? 'danger-bg' : 'success-bg'}`} style={{ width: `${progressData.estado === 'ATRASO' ? Math.min(100, atrasoRatio) : 100}%` }} />
        </div>
      </div>

      <div className="kpi-card progress-card meta-card">
        <div className="kpi-content">
          <h3 className="kpi-title">ENTREGABLES</h3>
          <div className="kpi-value warning">{toNumber(progressData.entregablesConformidad ?? progressData.entregablesConformes ?? 0)}<span className="kpi-total">/{toNumber(progressData.entregablesTotal ?? 0)}</span></div>
          <p className="kpi-detail lock-detail"><Lock size={12} /> conformes / totales</p>
        </div>
        <div className="kpi-progress-container">
          <div className="progress-bar-fill success-bg" style={{ width: `${toNumber(progressData.entregablesTotal) > 0 ? (toNumber(progressData.entregablesConformidad ?? progressData.entregablesConformes ?? 0) / toNumber(progressData.entregablesTotal)) * 100 : 0}%` }} />
        </div>
      </div>
    </div>
  );
};

export default ProgressKPIs;
