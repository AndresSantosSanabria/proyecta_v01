import { Lock, TrendingUp, TrendingDown, Percent } from 'lucide-react';

const toNumber = (value) => {
  if (value == null) return 0;
  const parsed = typeof value === 'string' ? Number.parseFloat(value) : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
};

const toDisplayPercent = (value) => {
  const numeric = toNumber(value);
  if (Math.abs(numeric) <= 1) {
    return numeric * 100;
  }
  return numeric;
};

const ProgressKPIs = ({ progressData }) => {
  const programado = toNumber(progressData.progresoProgramado ?? progressData.avanceProgramado ?? 0);
  const ejecutado = toNumber(progressData.progresoEjecutado ?? progressData.avanceTotal ?? 0);
  const diferencia = toNumber(progressData.diferencia ?? (programado - ejecutado));
  const eficacia = toNumber(progressData.eficacia ?? (programado === 0 ? 1 : ejecutado / programado));
  const eficiencia = toNumber(progressData.eficiencia ?? 0);

  const completionRatio = programado > 0 ? Math.min(100, (ejecutado / programado) * 100) : 100;
  const eficienciaRatio = Math.min(100, toDisplayPercent(eficiencia));
  const diferenciaTone = diferencia > 0 ? 'danger' : 'warning';
  const diferenciaSigno = diferencia > 0 ? '-' : '';
  const detalleDiferencia = (
    diferencia > 0
      ? <><TrendingDown size={12} /> atrasado respecto al plan</>
      : <><Lock size={12} /> en línea con el plan</>
  );
  const eficienciaTone = eficiencia >= 0.5 ? 'warning' : 'danger';
  const eficienciaBarTone = eficiencia >= 0.5 ? 'warning-bg' : 'danger-bg';

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
          <div className={`kpi-value ${diferencia < 0 ? 'success' : diferenciaTone}`}>
            {diferencia < 0 ? '+' : diferenciaSigno}{Math.abs(diferencia).toFixed(2)}%
          </div>
          <p className="kpi-detail lock-detail">
            {diferencia < 0
              ? <><TrendingUp size={12} /> adelantado al plan</>
              : detalleDiferencia}
          </p>
        </div>
        <div className="kpi-progress-container">
          <div
            className={`progress-bar-fill ${diferencia <= 0 ? 'success-bg' : 'danger-bg'}`}
            style={{ width: `${Math.min(100, Math.abs(diferencia))}%` }}
          />
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">EFICACIA</h3>
          <div className="kpi-value success">{toDisplayPercent(eficacia).toFixed(1)}%</div>
          <p className="kpi-detail lock-detail"><Percent size={12} /> entregados a fecha límite / programados a fecha límite</p>
        </div>
        <div className="kpi-progress-container">
          <div className="progress-bar-fill success-bg" style={{ width: `${Math.min(100, toDisplayPercent(eficacia))}%` }} />
        </div>
      </div>

      <div className="kpi-card progress-card">
        <div className="kpi-content">
          <h3 className="kpi-title">EFICIENCIA</h3>
          <div className={`kpi-value ${eficiencia >= 0.8 ? 'success' : eficienciaTone}`}>
            {toDisplayPercent(eficiencia).toFixed(1)}%
          </div>
          <p className="kpi-detail lock-detail"><TrendingDown size={12} /> entregados a tiempo / entregados a fecha límite</p>
        </div>
        <div className="kpi-progress-container">
          <div
            className={`progress-bar-fill ${eficiencia >= 0.8 ? 'success-bg' : eficienciaBarTone}`}
            style={{ width: `${eficienciaRatio}%` }}
          />
        </div>
      </div>

      <div className="kpi-card progress-card meta-card">
        <div className="kpi-content">
          <h3 className="kpi-title">ENTREGABLES</h3>
          <div className="kpi-value warning">
            {toNumber(progressData.entregablesConformidad ?? progressData.entregablesConformes ?? 0)}
            <span className="kpi-total">/{toNumber(progressData.entregablesTotal ?? 0)}</span>
          </div>
          <p className="kpi-detail lock-detail"><Lock size={12} /> conformes / totales</p>
        </div>
        <div className="kpi-progress-container">
          <div
            className="progress-bar-fill success-bg"
            style={{
              width: `${
                toNumber(progressData.entregablesTotal) > 0
                  ? (toNumber(progressData.entregablesConformidad ?? progressData.entregablesConformes ?? 0) / toNumber(progressData.entregablesTotal)) * 100
                  : 0
              }%`,
            }}
          />
        </div>
      </div>
    </div>
  );
};

export default ProgressKPIs;
