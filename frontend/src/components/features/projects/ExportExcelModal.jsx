import { useState, useMemo } from 'react';
import { Download, X, CheckSquare, Square } from 'lucide-react';

const STATE_CONFIG = [
  { key: 'PENDIENTE_COMPLETAR', label: 'Pendiente completar', color: '#f59e0b' },
  { key: 'ACTIVO', label: 'Activo', color: '#10b981' },
  { key: 'CON_RETRASOS', label: 'Con retrasos', color: '#ef4444' },
  { key: 'CERRADO', label: 'Cerrado', color: '#6b7280' },
  { key: 'CERRADO_FORZOSO', label: 'Cerrado forzoso', color: '#dc2626' },
  { key: 'EN_REVISION', label: 'En revisión', color: '#8b5cf6' },
  { key: 'FINALIZADO', label: 'Finalizado', color: '#0ea5e9' },
  { key: 'PENDIENTE_COMPLETAR:CARGADA', label: 'Docs. pendientes verificación', color: '#f97316' },
  { key: 'PENDIENTE_COMPLETAR:DEVUELTA', label: 'Docs. devueltos', color: '#e11d48' },
];

const ExportExcelModal = ({ isOpen, onClose, onExport, projects, exporting }) => {
  const [selected, setSelected] = useState(() => new Set(STATE_CONFIG.map((s) => s.key)));

  const stateCounts = useMemo(() => {
    const counts = {};
    STATE_CONFIG.forEach((s) => { counts[s.key] = 0; });

    (projects || []).forEach((p) => {
      const estado = p?.estado || '';
      const viab = p?.viabilidadEstado || '';

      if (estado === 'PENDIENTE_COMPLETAR') {
        if (viab === 'DEVUELTA') {
          counts['PENDIENTE_COMPLETAR:DEVUELTA']++;
        } else if (viab === 'CARGADA' || viab === 'APROBADA') {
          counts['PENDIENTE_COMPLETAR:CARGADA']++;
        } else {
          counts['PENDIENTE_COMPLETAR']++;
        }
      } else if (counts[estado] !== undefined) {
        counts[estado]++;
      }
    });

    return counts;
  }, [projects]);

  const totalProjects = projects?.length || 0;
  const selectedCount = useMemo(
    () => Array.from(selected).reduce((sum, key) => sum + (stateCounts[key] || 0), 0),
    [selected, stateCounts]
  );

  const allSelected = selected.size === STATE_CONFIG.length;
  const noneSelected = selected.size === 0;

  const toggleAll = () => {
    if (allSelected) {
      setSelected(new Set());
    } else {
      setSelected(new Set(STATE_CONFIG.map((s) => s.key)));
    }
  };

  const toggleState = (key) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  const handleExport = () => {
    const statuses = Array.from(selected).join(',');
    onExport(statuses);
  };

  if (!isOpen) return null;

  return (
    <div className="export-modal-overlay" onClick={onClose}>
      <div className="export-modal" onClick={(e) => e.stopPropagation()}>
        <div className="export-modal__header">
          <div className="export-modal__title">
            <Download size={18} />
            <h3>Exportar consolidado Excel</h3>
          </div>
          <button className="export-modal__close" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        <div className="export-modal__body">
          <p className="export-modal__description">
            Selecciona los estados de proyecto que deseas incluir en el archivo Excel.
          </p>

          <div className="export-modal__summary">
            <span className="export-modal__summary-text">
              <strong>{selectedCount}</strong> de <strong>{totalProjects}</strong> proyectos seleccionados
            </span>
            <button className="export-modal__toggle-all" onClick={toggleAll}>
              {allSelected ? <CheckSquare size={14} /> : <Square size={14} />}
              {allSelected ? 'Deseleccionar todo' : 'Seleccionar todo'}
            </button>
          </div>

          <div className="export-modal__states">
            {STATE_CONFIG.map((state) => {
              const count = stateCounts[state.key] || 0;
              const isSelected = selected.has(state.key);
              const pct = totalProjects > 0 ? (count / totalProjects) * 100 : 0;

              return (
                <button
                  key={state.key}
                  className={`export-modal__state ${isSelected ? 'export-modal__state--active' : ''}`}
                  onClick={() => toggleState(state.key)}
                >
                  <div className="export-modal__state-check">
                    {isSelected ? <CheckSquare size={16} /> : <Square size={16} />}
                  </div>
                  <div className="export-modal__state-info">
                    <div className="export-modal__state-label">
                      <span
                        className="export-modal__state-dot"
                        style={{ background: state.color }}
                      />
                      <span>{state.label}</span>
                    </div>
                    <div className="export-modal__state-bar">
                      <div
                        className="export-modal__state-bar-fill"
                        style={{ width: `${pct}%`, background: state.color }}
                      />
                    </div>
                  </div>
                  <span className="export-modal__state-count">{count}</span>
                </button>
              );
            })}
          </div>
        </div>

        <div className="export-modal__footer">
          <button className="export-modal__btn-cancel" onClick={onClose} disabled={exporting}>
            Cancelar
          </button>
          <button
            className="export-modal__btn-export"
            onClick={handleExport}
            disabled={exporting || noneSelected}
          >
            <Download size={14} />
            {exporting ? 'Exportando...' : `Exportar ${selectedCount} proyecto${selectedCount !== 1 ? 's' : ''}`}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExportExcelModal;
