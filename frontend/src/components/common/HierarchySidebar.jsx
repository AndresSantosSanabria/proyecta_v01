import { useState, useEffect } from 'react';
import { CheckCircle, AlertTriangle, ChevronRight, Layers, Flag, Package, X } from 'lucide-react';
import './HierarchySidebar.css';

const sumPonderacion = (items) => items.reduce((s, i) => s + (Number.parseFloat(i.ponderacion) || 0), 0);

const PesoTag = ({ value }) => {
  const num = Number.parseFloat(value) || 0;
  return <span className="hs-sidebar__peso">{num.toFixed(1)}%</span>;
};

const SumaBadge = ({ items }) => {
  const total = sumPonderacion(items);
  const ok = Math.abs(total - 100) < 0.01;
  return (
    <span className={`hs-sidebar__suma ${ok ? 'hs-sidebar__suma--ok' : 'hs-sidebar__suma--error'}`}>
      {ok ? <CheckCircle size={11} /> : <AlertTriangle size={11} />}
      {total.toFixed(1)}%
    </span>
  );
};

const collectHitKeys = (fases, target) => {
  fases.forEach((fase, fIdx) => {
    (fase.hitos || []).forEach((_, hIdx) => {
      target.add(`${fIdx}-${hIdx}`);
    });
  });
};

const HierarchySidebar = ({
  fases = [],
  onUpdateFase,
  onUpdateHito,
  onUpdateEntregable,
}) => {
  const [isOpen, setIsOpen] = useState(true);
  const [expandedFases, setExpandedFases] = useState(() => new Set(fases.map((_, i) => i)));
  const [expandedHitos, setExpandedHitos] = useState(() => {
    const keys = new Set();
    collectHitKeys(fases, keys);
    return keys;
  });

  useEffect(() => {
    setExpandedHitos((prev) => {
      const next = new Set(prev);
      collectHitKeys(fases, next);
      return next;
    });
  }, [fases]);

  const toggleFase = (idx) => {
    setExpandedFases((prev) => {
      const next = new Set(prev);
      next.has(idx) ? next.delete(idx) : next.add(idx);
      return next;
    });
  };

  const toggleHito = (key) => {
    setExpandedHitos((prev) => {
      const next = new Set(prev);
      next.has(key) ? next.delete(key) : next.add(key);
      return next;
    });
  };

  if (!isOpen) {
    return (
      <button
        type="button"
        className="hs-sidebar__toggle-closed"
        onClick={() => setIsOpen(true)}
        title="Mostrar estructura"
      >
        <Layers size={16} />
        <span>Estructura</span>
        <SumaBadge items={fases} />
      </button>
    );
  }

  return (
    <div className="hs-sidebar">
      <div className="hs-sidebar__header">
        <div className="hs-sidebar__header-left">
          <Layers size={14} />
          <span className="hs-sidebar__title">Estructura</span>
          <SumaBadge items={fases} />
        </div>
        <button
          type="button"
          className="hs-sidebar__close"
          onClick={() => setIsOpen(false)}
          title="Ocultar estructura"
        >
          <X size={14} />
        </button>
      </div>

      <div className="hs-sidebar__tree">
        {fases.length === 0 && (
          <span className="hs-sidebar__empty">Sin fases configuradas</span>
        )}

        {fases.map((fase, fIdx) => {
          const isExpanded = expandedFases.has(fIdx);
          const hitos = fase.hitos || [];

          return (
            <div key={fIdx} className="hs-sidebar__node hs-sidebar__node--fase">
              <div
                className="hs-sidebar__node-row"
                role="button"
                tabIndex={0}
                onClick={() => toggleFase(fIdx)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggleFase(fIdx);
                  }
                }}
              >
                <ChevronRight size={12} className={`hs-sidebar__chevron ${isExpanded ? 'hs-sidebar__chevron--open' : ''}`} />
                <Layers size={12} className="hs-sidebar__icon hs-sidebar__icon--fase" />
                <span className="hs-sidebar__label">{fase.nombre}</span>
                <PesoTag value={fase.ponderacion} />
                <SumaBadge items={hitos} />
              </div>

              {isExpanded && (
                <div className="hs-sidebar__children">
                  <div className="hs-sidebar__ponderacion-row">
                    <label htmlFor={`hs-ponderacion-fase-${fIdx}`}>Ponderacion:</label>
                    <input
                      type="number"
                      id={`hs-ponderacion-fase-${fIdx}`}
                      min="0"
                      max="100"
                      step="0.01"
                      className="hs-sidebar__input"
                      value={fase.ponderacion}
                      onChange={(e) => onUpdateFase(fIdx, 'ponderacion', e.target.value)}
                    />
                    <span>%</span>
                  </div>

                  {hitos.map((hito, hIdx) => {
                    const hKey = `${fIdx}-${hIdx}`;
                    const isHitoExpanded = expandedHitos.has(hKey);
                    const entregables = hito.entregables || [];

                    return (
                      <div key={hIdx} className="hs-sidebar__node hs-sidebar__node--hito">
                        <div
                          className="hs-sidebar__node-row"
                          role="button"
                          tabIndex={0}
                          onClick={() => toggleHito(hKey)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter' || e.key === ' ') {
                              e.preventDefault();
                              toggleHito(hKey);
                            }
                          }}
                        >
                          <ChevronRight size={11} className={`hs-sidebar__chevron ${isHitoExpanded ? 'hs-sidebar__chevron--open' : ''}`} />
                          <Flag size={11} className="hs-sidebar__icon hs-sidebar__icon--hito" />
                          <span className="hs-sidebar__label">{hito.nombre}</span>
                          <PesoTag value={hito.ponderacion} />
                          <SumaBadge items={entregables} />
                        </div>

                        {isHitoExpanded && (
                          <div className="hs-sidebar__children">
                            <div className="hs-sidebar__ponderacion-row">
                              <label htmlFor={`hs-ponderacion-hito-${fIdx}-${hIdx}`}>Ponderacion:</label>
                              <input
                                type="number"
                                id={`hs-ponderacion-hito-${fIdx}-${hIdx}`}
                                min="0"
                                max="100"
                                step="0.01"
                                className="hs-sidebar__input"
                                value={hito.ponderacion}
                                onChange={(e) => onUpdateHito(fIdx, hIdx, 'ponderacion', e.target.value)}
                              />
                              <span>%</span>
                            </div>

                            {entregables.map((ent, eIdx) => (
                              <div key={eIdx} className="hs-sidebar__node hs-sidebar__node--entregable">
                                <div className="hs-sidebar__node-row">
                                  <Package size={10} className="hs-sidebar__icon hs-sidebar__icon--entregable" />
                                  <span className="hs-sidebar__label">{ent.nombre}</span>
                                  <PesoTag value={ent.ponderacion} />
                                </div>
                                <div className="hs-sidebar__ponderacion-row">
                                  <label htmlFor={`hs-ponderacion-ent-${fIdx}-${hIdx}-${eIdx}`}>Ponderacion:</label>
                                  <input
                                    type="number"
                                    id={`hs-ponderacion-ent-${fIdx}-${hIdx}-${eIdx}`}
                                    min="0"
                                    max="100"
                                    step="0.01"
                                    className="hs-sidebar__input"
                                    value={ent.ponderacion}
                                    onChange={(e) => onUpdateEntregable(fIdx, hIdx, eIdx, 'ponderacion', e.target.value)}
                                  />
                                  <span>%</span>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default HierarchySidebar;
