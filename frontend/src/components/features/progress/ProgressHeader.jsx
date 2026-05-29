import { CalendarDays, ChevronRight, ShieldCheck } from 'lucide-react';

const ProgressHeader = ({ codigo, nombre, dependencia, estado, corte }) => {
  const statusLabel = estado === 'ATRASO' ? 'Con atraso' : 'En tiempo';
  const statusClass = estado === 'ATRASO' ? 'danger' : 'success';

  return (
    <header className="progress-header progress-hero">
      <div className="progress-hero-copy">
        <span className="progress-hero-kicker">Seguimiento ejecutivo</span>
        <div className="header-title">
          <h1>Avance del Proyecto</h1>
          <span className="project-id-badge">{codigo}</span>
        </div>
        <p className="progress-hero-description">
          Vista consolidada del estado, avance y entregables del proyecto {nombre || 'seleccionado'}.
        </p>
        <div className="progress-hero-chips">
          <span className="progress-chip">
            <ShieldCheck size={14} />
            {dependencia || 'Sin dependencia'}
          </span>
          <span className="progress-chip muted">
            <CalendarDays size={14} />
            Corte {corte || 'sin fecha'}
          </span>
          <span className={`progress-chip status ${statusClass}`}>{statusLabel}</span>
        </div>
      </div>

      <div className="progress-hero-side">
        <div className={`hero-status-card ${statusClass}`}>
          <span className="hero-status-label">Estado general</span>
          <strong>{statusLabel}</strong>
          <small>Monitoreo automático por semáforo de avance</small>
        </div>
        <div className="hero-side-link">
          <ChevronRight size={16} />
          <span>Resumen ejecutivo y desglose jerárquico</span>
        </div>
      </div>
    </header>
  );
};

export default ProgressHeader;
