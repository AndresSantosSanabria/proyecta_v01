import React from 'react';
import { Lock } from 'lucide-react';

const ProgressKPIs = ({ progressData }) => {
  return (
    <div className="progress-kpi-grid">
      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">AVANCE TOTAL DEL PROYECTO</h3>
          <div className="kpi-value warning">{progressData.avanceTotal}%</div>
          <p className="kpi-detail lock-detail">Calculado automáticamente <Lock size={12} /></p>
        </div>
        <div className="kpi-progress-container">
           <div className="progress-bar-fill" style={{ width: `${progressData.avanceTotal}%` }}></div>
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">ENTREGABLES A CONFORMIDAD</h3>
          <div className="kpi-value success">{progressData.entregablesConformidad}<span className="kpi-total">/{progressData.entregablesTotal}</span></div>
          <p className="kpi-detail lock-detail"><Lock size={12} /> calculado</p>
        </div>
        <div className="kpi-progress-container">
           <div className="progress-bar-fill success-bg" style={{ width: `${(progressData.entregablesConformidad/progressData.entregablesTotal)*100}%` }}></div>
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">ATRASADOS</h3>
          <div className="kpi-value danger">{progressData.atrasados}</div>
          <p className="kpi-detail lock-detail">Fecha vencida <Lock size={12} /></p>
        </div>
        <div className="kpi-progress-container">
           <div className="progress-bar-fill danger-bg" style={{ width: '100%' }}></div>
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">PRÓXIMOS A VENCER</h3>
          <div className="kpi-value success">{progressData.proximosVencer}</div>
          <p className="kpi-detail lock-detail">Vencen en ≤ 5 días <Lock size={12} /></p>
        </div>
        <div className="kpi-progress-container">
           <div className="progress-bar-fill success-bg" style={{ width: '100%' }}></div>
        </div>
      </div>
    </div>
  );
};

export default ProgressKPIs;
