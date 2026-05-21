import React from 'react';
import { AlertTriangle, CheckCircle } from 'lucide-react';

const sumPonderacion = (items) => items.reduce((s, i) => s + (parseFloat(i.ponderacion) || 0), 0);

const PesoIndicator = ({ actual }) => {
  const ok = Math.abs(actual - 100) < 0.01;
  return (
    <span className={`peso-indicator ${ok ? 'peso-ok' : 'peso-error'}`}>
      {ok ? <CheckCircle size={14} /> : <AlertTriangle size={14} />}
      Suma: {actual.toFixed(2)}%
    </span>
  );
};

const Paso3FasesHitosEntregables = ({ data, onChange, errors }) => {
  const fases = data.fases || [];

  const updateFase = (fIndex, field, value) => {
    const nuevas = [...fases];
    nuevas[fIndex] = { ...nuevas[fIndex], [field]: value };
    onChange({ fases: nuevas });
  };

  const addFase = () => {
    onChange({ fases: [...fases, { nombre: '', descripcion: '', ponderacion: '', hitos: [] }] });
  };

  const removeFase = (fIndex) => {
    onChange({ fases: fases.filter((_, i) => i !== fIndex) });
  };

  const addHito = (fIndex) => {
    const nuevas = [...fases];
    nuevas[fIndex] = {
      ...nuevas[fIndex],
      hitos: [...(nuevas[fIndex].hitos || []), { nombre: '', descripcion: '', ponderacion: '', entregables: [] }],
    };
    onChange({ fases: nuevas });
  };

  const updateHito = (fIndex, hIndex, field, value) => {
    const nuevas = [...fases];
    const hitos = [...(nuevas[fIndex].hitos || [])];
    hitos[hIndex] = { ...hitos[hIndex], [field]: value };
    nuevas[fIndex] = { ...nuevas[fIndex], hitos };
    onChange({ fases: nuevas });
  };

  const removeHito = (fIndex, hIndex) => {
    const nuevas = [...fases];
    nuevas[fIndex] = {
      ...nuevas[fIndex],
      hitos: (nuevas[fIndex].hitos || []).filter((_, i) => i !== hIndex),
    };
    onChange({ fases: nuevas });
  };

  const addEntregable = (fIndex, hIndex) => {
    const nuevas = [...fases];
    const hitos = [...(nuevas[fIndex].hitos || [])];
    hitos[hIndex] = {
      ...hitos[hIndex],
      entregables: [...(hitos[hIndex].entregables || []), { nombre: '', ponderacion: '', fechaLimite: '' }],
    };
    nuevas[fIndex] = { ...nuevas[fIndex], hitos };
    onChange({ fases: nuevas });
  };

  const updateEntregable = (fIndex, hIndex, eIndex, field, value) => {
    const nuevas = [...fases];
    const hitos = [...(nuevas[fIndex].hitos || [])];
    const entregables = [...(hitos[hIndex].entregables || [])];
    entregables[eIndex] = { ...entregables[eIndex], [field]: value };
    hitos[hIndex] = { ...hitos[hIndex], entregables };
    nuevas[fIndex] = { ...nuevas[fIndex], hitos };
    onChange({ fases: nuevas });
  };

  const removeEntregable = (fIndex, hIndex, eIndex) => {
    const nuevas = [...fases];
    const hitos = [...(nuevas[fIndex].hitos || [])];
    hitos[hIndex] = {
      ...hitos[hIndex],
      entregables: (hitos[hIndex].entregables || []).filter((_, i) => i !== eIndex),
    };
    nuevas[fIndex] = { ...nuevas[fIndex], hitos };
    onChange({ fases: nuevas });
  };

  const sumaFases = sumPonderacion(fases);

  return (
    <div className="step-form">
      <div className="step-header-row">
        <h3 className="step-title">Configuración de Fases, Hitos y Entregables</h3>
        <PesoIndicator actual={sumaFases} />
      </div>
      <p className="help-text">La suma de ponderaciones en cada nivel debe ser exactamente 100%.</p>

      {fases.map((fase, fIndex) => {
        const sumaHitos = sumPonderacion(fase.hitos || []);
        return (
          <div key={fIndex} className="jerarquia-card fase-card">
            <div className="jerarquia-card-header">
              <strong>Fase #{fIndex + 1}</strong>
              <div className="jerarquia-card-actions">
                <PesoIndicator actual={sumaHitos} />
                <button type="button" className="btn-icon-danger" onClick={() => removeFase(fIndex)}>✕</button>
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Nombre de la Fase *</label>
                <input
                  className={`form-input ${errors[`fase_${fIndex}_nombre`] ? 'input-error' : ''}`}
                  value={fase.nombre || ''}
                  onChange={(e) => updateFase(fIndex, 'nombre', e.target.value)}
                  placeholder="Ej: Planeación"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Ponderación (%) *</label>
                <input
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  className={`form-input ${errors[`fase_${fIndex}_ponderacion`] ? 'input-error' : ''}`}
                  value={fase.ponderacion}
                  onChange={(e) => updateFase(fIndex, 'ponderacion', e.target.value)}
                  placeholder="Ej: 50"
                />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Descripción</label>
              <input
                className="form-input"
                value={fase.descripcion || ''}
                onChange={(e) => updateFase(fIndex, 'descripcion', e.target.value)}
                placeholder="Descripción opcional de la fase"
              />
            </div>

            {/* HITOS */}
            {(fase.hitos || []).map((hito, hIndex) => {
              const sumaEntregables = sumPonderacion(hito.entregables || []);
              return (
                <div key={hIndex} className="jerarquia-card hito-card">
                  <div className="jerarquia-card-header">
                    <strong>Hito #{fIndex + 1}.{hIndex + 1}</strong>
                    <div className="jerarquia-card-actions">
                      <PesoIndicator actual={sumaEntregables} />
                      <button type="button" className="btn-icon-danger" onClick={() => removeHito(fIndex, hIndex)}>✕</button>
                    </div>
                  </div>
                  <div className="form-grid">
                    <div className="form-group">
                      <label className="form-label">Nombre del Hito *</label>
                      <input
                        className="form-input"
                        value={hito.nombre || ''}
                        onChange={(e) => updateHito(fIndex, hIndex, 'nombre', e.target.value)}
                        placeholder="Ej: Cierre de planeación"
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Ponderación (%) *</label>
                      <input
                        type="number"
                        min="0"
                        max="100"
                        step="0.01"
                        className="form-input"
                        value={hito.ponderacion}
                        onChange={(e) => updateHito(fIndex, hIndex, 'ponderacion', e.target.value)}
                        placeholder="Ej: 100"
                      />
                    </div>
                  </div>

                  {/* ENTREGABLES */}
                  {(hito.entregables || []).map((ent, eIndex) => (
                    <div key={eIndex} className="jerarquia-card entregable-card">
                      <div className="jerarquia-card-header">
                        <strong>Entregable #{fIndex + 1}.{hIndex + 1}.{eIndex + 1}</strong>
                        <button type="button" className="btn-icon-danger" onClick={() => removeEntregable(fIndex, hIndex, eIndex)}>✕</button>
                      </div>
                      <div className="form-grid">
                        <div className="form-group">
                          <label className="form-label">Nombre *</label>
                          <input
                            className="form-input"
                            value={ent.nombre || ''}
                            onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'nombre', e.target.value)}
                            placeholder="Ej: Acta de inicio"
                          />
                        </div>
                        <div className="form-group">
                          <label className="form-label">Ponderación (%) *</label>
                          <input
                            type="number"
                            min="0"
                            max="100"
                            step="0.01"
                            className="form-input"
                            value={ent.ponderacion}
                            onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'ponderacion', e.target.value)}
                            placeholder="Ej: 100"
                          />
                        </div>
                        <div className="form-group">
                          <label className="form-label">Fecha Límite *</label>
                          <input
                            type="date"
                            className="form-input"
                            value={ent.fechaLimite || ''}
                            onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'fechaLimite', e.target.value)}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                  <button type="button" className="btn-add btn-add-sm" onClick={() => addEntregable(fIndex, hIndex)}>
                    + Agregar entregable
                  </button>
                </div>
              );
            })}
            <button type="button" className="btn-add btn-add-sm" onClick={() => addHito(fIndex)}>
              + Agregar hito
            </button>
          </div>
        );
      })}

      <button type="button" className="btn-add" onClick={addFase}>
        + Agregar fase
      </button>
    </div>
  );
};

export default Paso3FasesHitosEntregables;
