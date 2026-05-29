import { ChevronDown, ChevronUp, Info } from 'lucide-react';

const ProjectInfoCard = ({ displayResumen, expanded, onToggleExpanded }) => {
  const formatPercent = (value) => {
    if (value === null || value === undefined || value === '') return '0%';
    if (typeof value === 'string' && value.trim().endsWith('%')) return value;
    const numeric = Number(value);
    return Number.isFinite(numeric) ? `${numeric}%` : String(value);
  };

  const compactItems = [
    { label: 'Fecha inicio', value: displayResumen.fechaInicio, tone: 'neutral' },
    { label: 'Director', value: displayResumen.director, tone: 'neutral' },
    { label: 'Fases', value: displayResumen.fases, tone: 'accent' },
    { label: 'Hitos', value: displayResumen.totalHitos, tone: 'accent' },
    { label: 'Avance', value: displayResumen.avance, tone: 'success' },
  ];

  const detailItems = [
    { label: 'Meta', value: displayResumen.meta },
    { label: 'Dependencia', value: displayResumen.dependencia },
    { label: 'Programado', value: displayResumen.programado },
    { label: 'Programados al corte', value: displayResumen.programadosAlCorte },
    { label: 'Entregados al corte', value: displayResumen.entregadosAlCorte },
    { label: 'Eficacia', value: formatPercent(displayResumen.eficacia) },
    { label: 'Eficiencia', value: formatPercent(displayResumen.eficiencia) },
  ];

  return (
    <div className="cronograma-card project-info-card">
      <div className="card-topline">
        <div className="card-headline">
          <h3 className="card-title">
            <Info size={18} />
            Información del Proyecto
          </h3>
          <p className="card-caption">
            Resumen ejecutivo breve, con detalle desplegable para la información extendida.
          </p>
        </div>

        <button
          type="button"
          className="summary-toggle"
          onClick={onToggleExpanded}
          aria-expanded={expanded}
        >
          {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
          {expanded ? 'Ocultar detalle' : 'Ver detalle'}
        </button>
      </div>

      <div className="info-list compact">
        {compactItems.map((item) => (
          <div key={item.label} className={`info-chip info-chip-${item.tone}`}>
            <span className="info-label">{item.label}</span>
            <span className="info-value summary-clamp">{item.value}</span>
          </div>
        ))}
      </div>

      {expanded && (
        <div className="info-detail-panel">
          {detailItems.map((item) => (
            <div key={item.label} className="info-detail-item">
              <span>{item.label}</span>
              <strong className="summary-clamp">{item.value}</strong>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ProjectInfoCard;
