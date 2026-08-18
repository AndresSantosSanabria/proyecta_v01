import { AlertTriangle, CheckCircle, Flag, Layers, Package } from 'lucide-react';

const sumPonderacion = (items) => items.reduce((s, i) => s + (parseFloat(i.ponderacion) || 0), 0);
const hasPersistentId = (item) => Boolean(item?.id || item?.faseId || item?.hitoId || item?.entregableId);

const getRemainingWeight = (items) => {
  const remaining = 100 - sumPonderacion(items || []);
  if (remaining <= 0) return '';
  return Number.isInteger(remaining) ? String(remaining) : remaining.toFixed(2);
};

const createEntregable = (ponderacion = '100', numero = 1) => ({
  nombre: `E${String(numero).padStart(2, '0')}`,
  descripcion: '',
  ponderacion,
  fechaInicio: '',
  fechaLimite: '',
});
const createHito = (ponderacion = '100', numero = 1) => ({
  nombre: `H${String(numero).padStart(2, '0')}`,
  descripcion: '',
  ponderacion,
  entregables: [createEntregable()],
});
const createFase = (ponderacion = '100', numero = 1) => ({
  nombre: `F${String(numero).padStart(2, '0')}`,
  descripcion: '',
  ponderacion,
  hitos: [createHito()],
});

const normalizeHierarchyNames = (fases = []) => {
  let hitoCounter = 0;
  let entregableCounter = 0;
  return (fases || []).map((fase, faseIndex) => ({
    ...fase,
    nombre: `F${String(faseIndex + 1).padStart(2, '0')}`,
    hitos: (fase.hitos || []).map((hito) => {
      hitoCounter += 1;
      const entregables = (hito.entregables || []).map((entregable) => {
        entregableCounter += 1;
        return {
          ...entregable,
          nombre: `E${String(entregableCounter).padStart(2, '0')}`,
        };
      });
      return {
        ...hito,
        nombre: `H${String(hitoCounter).padStart(2, '0')}`,
        entregables,
      };
    }),
  }));
};

const getProjectStartDate = (data) => data?.fechaInicioProyecto || data?.fechaInicio || data?.projectStartDate || '';

const PesoIndicator = ({ actual }) => {
  const ok = Math.abs(actual - 100) < 0.01;
  return (
    <span className={`peso-indicator ${ok ? 'peso-ok' : 'peso-error'}`}>
      {ok ? <CheckCircle size={14} /> : <AlertTriangle size={14} />}
      Suma: {actual.toFixed(2)}%
    </span>
  );
};

const Paso3FasesHitosEntregables = ({
  data,
  onChange,
  errors = {},
  lockExistingDates = false,
  protectExistingItems = false,
  allowEmpty = true,
  onFileChange,
}) => {
  const fases = normalizeHierarchyNames(data.fases || []);
  const projectStartDate = getProjectStartDate(data);

  const handleFileChange = (fIndex, hIndex, eIndex, event) => {
    const file = event.target.files?.[0];
    if (onFileChange) {
      onFileChange(fIndex, hIndex, eIndex, file);
    }
  };

  const isRetroactiveDate = (fechaLimite) => {
    if (!fechaLimite) return false;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const fecha = new Date(`${fechaLimite}T00:00:00`);
    return fecha < hoy;
  };

  const updateFase = (fIndex, field, value) => {
    const nuevas = [...fases];
    nuevas[fIndex] = { ...nuevas[fIndex], [field]: value };
    onChange({ fases: nuevas });
  };

  const addFase = () => {
    onChange({ fases: [...fases, createFase(getRemainingWeight(fases), fases.length + 1)] });
  };

  const removeFase = (fIndex) => {
    if (protectExistingItems && hasPersistentId(fases[fIndex])) return;
    onChange({ fases: fases.filter((_, i) => i !== fIndex) });
  };

  const addHito = (fIndex) => {
    const nuevas = [...fases];
    const hitosActuales = nuevas[fIndex].hitos || [];
    const totalHitos = nuevas.reduce((sum, f) => sum + (f.hitos || []).length, 0);
    nuevas[fIndex] = {
      ...nuevas[fIndex],
      hitos: [...hitosActuales, createHito(getRemainingWeight(hitosActuales), totalHitos + 1)],
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
    const hito = (fases[fIndex].hitos || [])[hIndex];
    if (protectExistingItems && hasPersistentId(hito)) return;
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
    const entregablesActuales = hitos[hIndex].entregables || [];
    const totalEntregables = nuevas.reduce((sum, f) =>
      sum + (f.hitos || []).reduce((s, h) => s + (h.entregables || []).length, 0), 0);
    hitos[hIndex] = {
      ...hitos[hIndex],
      entregables: [...entregablesActuales, createEntregable(getRemainingWeight(entregablesActuales), totalEntregables + 1)],
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
    const entregable = ((fases[fIndex].hitos || [])[hIndex]?.entregables || [])[eIndex];
    if (protectExistingItems && hasPersistentId(entregable)) return;
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
        <h3 className="step-title">Configuracion de Fases, Hitos y Entregables</h3>
        {fases.length > 0 && <PesoIndicator actual={sumaFases} />}
      </div>
      <p className="help-text">
        Puedes dejar esta seccion vacia al crear el proyecto. Cuando agregues estructura, cada fase debe tener al menos un hito y cada hito al menos un entregable.
      </p>

      <div className="hierarchy-rule-banner">
        <AlertTriangle size={16} />
        <span>
          <strong>Regla de ponderacion:</strong> la suma de fases debe ser 100%. Lo mismo aplica para hitos dentro de cada fase y entregables dentro de cada hito.
        </span>
      </div>

      <div className="hierarchy-summary-grid">
        <article className="hierarchy-summary-card fases">
          <Layers size={22} />
          <strong>Fases</strong>
          <span>Contenedores principales del proyecto. La suma total debe ser 100%.</span>
        </article>
        <article className="hierarchy-summary-card hitos">
          <Flag size={22} />
          <strong>Hitos</strong>
          <span>Puntos de control por fase. Cada fase debe sumar 100%.</span>
        </article>
        <article className="hierarchy-summary-card entregables">
          <Package size={22} />
          <strong>Entregables</strong>
          <span>Resultados verificables. Cada hito debe sumar 100%.</span>
        </article>
      </div>

      {fases.length === 0 && (
        <div className="hierarchy-empty-state">
          <strong>Sin jerarquia configurada por ahora.</strong>
          <span>
            {allowEmpty
              ? 'Puedes continuar y configurarla mas adelante desde el avance del proyecto.'
              : 'Agrega una fase para comenzar la estructura del proyecto.'}
          </span>
        </div>
      )}

      {fases.map((fase, fIndex) => {
        const sumaHitos = sumPonderacion(fase.hitos || []);
        const canRemoveFase = !protectExistingItems || !hasPersistentId(fase);

        return (
          <div key={fase.id || `fase-${fIndex}`} className="jerarquia-card fase-card">
            <div className="jerarquia-card-header">
              <strong>Fase #{fIndex + 1}</strong>
              <div className="jerarquia-card-actions">
                <PesoIndicator actual={sumaHitos} />
                {canRemoveFase && (
                  <button type="button" className="btn-icon-danger" onClick={() => removeFase(fIndex)}>x</button>
                )}
              </div>
            </div>
            <div className="form-grid">
              <div className="form-group">
                <label className="form-label">Nombre de la Fase *</label>
                <input
                  className={`form-input form-input-muted form-input-code ${errors[`fase_${fIndex}_nombre`] ? 'input-error' : ''}`}
                  value={fase.nombre || ''}
                  readOnly
                  aria-readonly="true"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Ponderacion (%) *</label>
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
              <label className="form-label">Descripcion</label>
              <input
                className="form-input"
                value={fase.descripcion || ''}
                onChange={(e) => updateFase(fIndex, 'descripcion', e.target.value)}
                placeholder="Descripcion opcional de la fase"
              />
            </div>

            {(fase.hitos || []).map((hito, hIndex) => {
              const sumaEntregables = sumPonderacion(hito.entregables || []);
              const canRemoveHito = !protectExistingItems || !hasPersistentId(hito);

              return (
                <div key={hito.id || `hito-${fIndex}-${hIndex}`} className="jerarquia-card hito-card">
                  <div className="jerarquia-card-header">
                    <strong>Hito #{fIndex + 1}.{hIndex + 1}</strong>
                    <div className="jerarquia-card-actions">
                      <PesoIndicator actual={sumaEntregables} />
                      {canRemoveHito && (
                        <button type="button" className="btn-icon-danger" onClick={() => removeHito(fIndex, hIndex)}>x</button>
                      )}
                    </div>
                  </div>
                  <div className="form-grid">
                    <div className="form-group">
                      <label className="form-label">Nombre del Hito *</label>
                      <input
                        className={`form-input form-input-muted form-input-code ${errors[`hito_${fIndex}_${hIndex}_nombre`] ? 'input-error' : ''}`}
                        value={hito.nombre || ''}
                        readOnly
                        aria-readonly="true"
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Ponderacion (%) *</label>
                      <input
                        type="number"
                        min="0"
                        max="100"
                        step="0.01"
                        className={`form-input ${errors[`hito_${fIndex}_${hIndex}_ponderacion`] ? 'input-error' : ''}`}
                        value={hito.ponderacion}
                        onChange={(e) => updateHito(fIndex, hIndex, 'ponderacion', e.target.value)}
                        placeholder="Ej: 100"
                      />
                    </div>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Descripcion</label>
                    <input
                      className="form-input"
                      value={hito.descripcion || ''}
                      onChange={(e) => updateHito(fIndex, hIndex, 'descripcion', e.target.value)}
                      placeholder="Descripcion opcional del hito"
                    />
                  </div>

                  {(hito.entregables || []).map((ent, eIndex) => {
                    const canRemoveEntregable = !protectExistingItems || !hasPersistentId(ent);
                    const dateLocked = lockExistingDates && hasPersistentId(ent);

                    return (
                      <div key={ent.id || `entregable-${fIndex}-${hIndex}-${eIndex}`} className="jerarquia-card entregable-card">
                        <div className="jerarquia-card-header">
                          <strong>Entregable #{fIndex + 1}.{hIndex + 1}.{eIndex + 1}</strong>
                          {canRemoveEntregable && (
                            <button type="button" className="btn-icon-danger" onClick={() => removeEntregable(fIndex, hIndex, eIndex)}>x</button>
                          )}
                        </div>
                        <div className="form-grid">
                          <div className="form-group">
                            <label className="form-label">Nombre *</label>
                            <input
                              className={`form-input form-input-muted form-input-code ${errors[`ent_${fIndex}_${hIndex}_${eIndex}_nombre`] ? 'input-error' : ''}`}
                              value={ent.nombre || ''}
                              readOnly
                              aria-readonly="true"
                            />
                          </div>
                          <div className="form-group">
                            <label className="form-label">Ponderacion (%) *</label>
                            <input
                              type="number"
                              min="0"
                              max="100"
                              step="0.01"
                              className={`form-input ${errors[`ent_${fIndex}_${hIndex}_${eIndex}_ponderacion`] ? 'input-error' : ''}`}
                              value={ent.ponderacion}
                              onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'ponderacion', e.target.value)}
                              placeholder="Ej: 100"
                            />
                          </div>
                          <div className="form-group">
                            <label className="form-label">Fecha inicio *</label>
                            <input
                              type="date"
                              className={`form-input ${dateLocked ? 'form-input-muted' : ''} ${errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaInicio`] ? 'input-error' : ''}`}
                              value={ent.fechaInicio || ''}
                              onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'fechaInicio', e.target.value)}
                              disabled={dateLocked}
                            />
                            {projectStartDate && !dateLocked && !isRetroactiveDate(ent.fechaLimite) && (
                              <span className="field-help">No puede iniciar antes del inicio del proyecto: {projectStartDate}.</span>
                            )}
                            {!dateLocked && isRetroactiveDate(ent.fechaLimite) && (
                              <span className="field-help field-help-warning">Este entregable es retroactivo: puedes seleccionar fechas anteriores a hoy.</span>
                            )}
                          </div>
                          <div className="form-group">
                            <label className="form-label">Fecha límite *</label>
                            <input
                              type="date"
                              className={`form-input ${dateLocked ? 'form-input-muted' : ''} ${errors[`ent_${fIndex}_${hIndex}_${eIndex}_fechaLimite`] ? 'input-error' : ''}`}
                              value={ent.fechaLimite || ''}
                              onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'fechaLimite', e.target.value)}
                              disabled={dateLocked}
                            />
                            {dateLocked && (
                              <span className="field-help">Las fechas no se pueden editar en entregables existentes.</span>
                            )}
                            {!dateLocked && isRetroactiveDate(ent.fechaLimite) && (
                              <span className="field-help field-help-warning">Entregable retroactivo: debes adjuntar un archivo de soporte que lo respalde.</span>
                            )}
                          </div>
                          {!dateLocked && isRetroactiveDate(ent.fechaLimite) && (
                            <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                              <label className="form-label">
                                Archivo de soporte (PDF) *
                                <span className="retroactive-badge">Obligatorio (entregable retroactivo)</span>
                              </label>
                              <input
                                type="file"
                                accept=".pdf"
                                className={`form-input ${errors[`ent_${fIndex}_${hIndex}_${eIndex}_archivo`] ? 'input-error' : ''}`}
                                onChange={(e) => handleFileChange(fIndex, hIndex, eIndex, e)}
                              />
                              {errors[`ent_${fIndex}_${hIndex}_${eIndex}_archivo`] && (
                                <span className="error-text">{errors[`ent_${fIndex}_${hIndex}_${eIndex}_archivo`]}</span>
                              )}
                            </div>
                          )}
                          <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                            <label className="form-label">Descripcion</label>
                            <input
                              className="form-input"
                              value={ent.descripcion || ''}
                              onChange={(e) => updateEntregable(fIndex, hIndex, eIndex, 'descripcion', e.target.value)}
                              placeholder="Descripcion opcional del entregable"
                            />
                          </div>
                        </div>
                      </div>
                    );
                  })}
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
