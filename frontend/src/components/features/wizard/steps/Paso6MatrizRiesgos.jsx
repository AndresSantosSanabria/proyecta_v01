import { Plus, Trash2, ShieldAlert, Shield, AlertTriangle } from 'lucide-react';
import './Paso6MatrizRiesgos.css';

const PROBABILIDADES = [
  { value: 'UNO', label: '1 - Muy baja' },
  { value: 'DOS', label: '2 - Baja' },
  { value: 'TRES', label: '3 - Media' },
  { value: 'CUATRO', label: '4 - Alta' },
  { value: 'CINCO', label: '5 - Muy alta' },
];

const IMPACTOS = [
  { value: 'UNO', label: '1 - Muy bajo' },
  { value: 'DOS', label: '2 - Bajo' },
  { value: 'TRES', label: '3 - Medio' },
  { value: 'CUATRO', label: '4 - Alto' },
  { value: 'CINCO', label: '5 - Muy alto' },
];

const P_MAP = { UNO: 1, DOS: 2, TRES: 3, CUATRO: 4, CINCO: 5 };
const I_MAP = { UNO: 1, DOS: 2, TRES: 3, CUATRO: 4, CINCO: 5 };

const calcScore = (prob, imp) => (P_MAP[prob] || 0) + (I_MAP[imp] || 0);

const calcLevel = (score) => {
  if (score >= 2 && score <= 4) return 'BAJO';
  if (score >= 5 && score <= 6) return 'MODERADO';
  if (score >= 7 && score <= 8) return 'ALTO';
  return 'EXTREMO';
};

const LEVEL_COLORS = {
  BAJO: { bg: '#dcfce7', color: '#166534' },
  MODERADO: { bg: '#fef9c3', color: '#854d0e' },
  ALTO: { bg: '#fed7aa', color: '#9a3412' },
  EXTREMO: { bg: '#fecaca', color: '#991b1b' },
};

const TIPOS_RIESGO = [
  { value: 'GENERAL', label: 'Riesgo General' },
  { value: 'SEGURIDAD', label: 'Riesgo de Seguridad' },
];

const emptyRiesgo = (tipo = 'GENERAL') => ({
  descripcion: '',
  probabilidad: 'TRES',
  impacto: 'TRES',
  entidadResponsable: '',
  tipoRiesgo: tipo,
});

const Paso6MatrizRiesgos = ({ data, onChange, errors }) => {
  const riesgos = data.riesgosIniciales || [];
  const riesgosGeneral = riesgos.filter((r) => (r.tipoRiesgo || 'GENERAL') === 'GENERAL');
  const riesgosSeguridad = riesgos.filter((r) => r.tipoRiesgo === 'SEGURIDAD');

  const handleChange = (index, field, value) => {
    const next = [...riesgos];
    next[index] = { ...next[index], [field]: value };
    onChange({ riesgosIniciales: next });
  };

  const handleAdd = (tipo) => {
    onChange({ riesgosIniciales: [...riesgos, emptyRiesgo(tipo)] });
  };

  const handleRemove = (index) => {
    onChange({ riesgosIniciales: riesgos.filter((_, i) => i !== index) });
  };

  const renderRiesgo = (riesgo, globalIndex) => {
    const idx = riesgos.indexOf(riesgo);
    return (
      <div key={idx} className="riesgo-card">
        <div className="riesgo-card-header">
          <div className="riesgo-card-icon">
            <ShieldAlert size={17} strokeWidth={2} />
          </div>
          <div className="riesgo-card-title">
            <h4>
              Riesgo {globalIndex + 1}
              <span className="riesgo-required">Requerido</span>
            </h4>
          </div>
          <button
            type="button"
            className="riesgo-card-remove"
            onClick={() => handleRemove(idx)}
            aria-label={`Eliminar riesgo ${globalIndex + 1}`}
          >
            <Trash2 size={14} strokeWidth={2.5} />
          </button>
        </div>

        <div className="riesgo-card-body">
          <div className="riesgo-field">
            <label className="form-label">Descripción del riesgo *</label>
            <textarea
              className={`form-input form-textarea ${errors[`riesgo_${idx}_descripcion`] ? 'input-error' : ''}`}
              value={riesgo.descripcion}
              onChange={(e) => handleChange(idx, 'descripcion', e.target.value)}
              rows={3}
              placeholder="Describa el riesgo identificado"
            />
            {errors[`riesgo_${idx}_descripcion`] && (
              <span className="error-text">{errors[`riesgo_${idx}_descripcion`]}</span>
            )}
          </div>

          <div className="riesgo-field-row">
            <div className="riesgo-field">
              <label className="form-label">Probabilidad *</label>
              <select
                className={`form-input ${errors[`riesgo_${idx}_probabilidad`] ? 'input-error' : ''}`}
                value={riesgo.probabilidad}
                onChange={(e) => handleChange(idx, 'probabilidad', e.target.value)}
              >
                {PROBABILIDADES.map((p) => (
                  <option key={p.value} value={p.value}>{p.label}</option>
                ))}
              </select>
              {errors[`riesgo_${idx}_probabilidad`] && (
                <span className="error-text">{errors[`riesgo_${idx}_probabilidad`]}</span>
              )}
            </div>

            <div className="riesgo-field">
              <label className="form-label">Impacto *</label>
              <select
                className={`form-input ${errors[`riesgo_${idx}_impacto`] ? 'input-error' : ''}`}
                value={riesgo.impacto}
                onChange={(e) => handleChange(idx, 'impacto', e.target.value)}
              >
                {IMPACTOS.map((i) => (
                  <option key={i.value} value={i.value}>{i.label}</option>
                ))}
              </select>
              {errors[`riesgo_${idx}_impacto`] && (
                <span className="error-text">{errors[`riesgo_${idx}_impacto`]}</span>
              )}
            </div>
          </div>

          <div className="riesgo-field-row">
            <div className="riesgo-field">
              <label className="form-label">Calificación (P + I)</label>
              <input
                type="text"
                className="form-input form-input-readonly"
                value={calcScore(riesgo.probabilidad, riesgo.impacto) || '—'}
                readOnly
              />
            </div>
            <div className="riesgo-field">
              <label className="form-label">Nivel de riesgo</label>
              {(() => {
                const score = calcScore(riesgo.probabilidad, riesgo.impacto);
                const level = calcLevel(score);
                const colors = LEVEL_COLORS[level] || LEVEL_COLORS.BAJO;
                return (
                  <input
                    type="text"
                    className="form-input form-input-readonly"
                    value={level}
                    readOnly
                    style={{ background: colors.bg, color: colors.color, fontWeight: 700, border: `1px solid ${colors.color}22` }}
                  />
                );
              })()}
            </div>
          </div>

          <div className="riesgo-field">
            <label className="form-label">Responsable</label>
            <input
              type="text"
              className="form-input"
              value={riesgo.entidadResponsable}
              onChange={(e) => handleChange(idx, 'entidadResponsable', e.target.value)}
              placeholder="Entidad o persona responsable"
            />
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Matriz de Riesgos</h3>
      <p className="help-text">
        Registre al menos un riesgo general y un riesgo de seguridad para el proyecto. Estos quedarán pendientes de tratamiento y podrán gestionarse una vez completado el proyecto.
      </p>

      {errors.riesgosIniciales && (
        <span className="error-text">{errors.riesgosIniciales}</span>
      )}

      {/* ── Riesgos Generales ────────────────────── */}
      <div className="riesgo-section">
        <div className="riesgo-section-header riesgo-section-header--general">
          <Shield size={16} />
          <span className="riesgo-section-title">Riesgos Generales</span>
          <span className="riesgo-section-badge">{riesgosGeneral.length}</span>
          {errors.riesgoGeneral && (
            <span className="riesgo-section-error">{errors.riesgoGeneral}</span>
          )}
        </div>

        {riesgosGeneral.length === 0 && (
          <div className="riesgo-section-empty">
            <AlertTriangle size={14} />
            <span>Debe registrar al menos un riesgo general.</span>
          </div>
        )}

        <div className="riesgo-list">
          {riesgosGeneral.map((r, i) => renderRiesgo(r, i))}
        </div>

        <button type="button" className="btn-add-riesgo" onClick={() => handleAdd('GENERAL')}>
          <Plus size={16} strokeWidth={2.5} />
          Agregar riesgo general
        </button>
      </div>

      {/* ── Riesgos de Seguridad ────────────────── */}
      <div className="riesgo-section">
        <div className="riesgo-section-header riesgo-section-header--seguridad">
          <ShieldAlert size={16} />
          <span className="riesgo-section-title">Riesgos de Seguridad</span>
          <span className="riesgo-section-badge">{riesgosSeguridad.length}</span>
          {errors.riesgoSeguridad && (
            <span className="riesgo-section-error">{errors.riesgoSeguridad}</span>
          )}
        </div>

        {riesgosSeguridad.length === 0 && (
          <div className="riesgo-section-empty">
            <AlertTriangle size={14} />
            <span>Debe registrar al menos un riesgo de seguridad.</span>
          </div>
        )}

        <div className="riesgo-list">
          {riesgosSeguridad.map((r, i) => renderRiesgo(r, i))}
        </div>

        <button type="button" className="btn-add-riesgo" onClick={() => handleAdd('SEGURIDAD')}>
          <Plus size={16} strokeWidth={2.5} />
          Agregar riesgo de seguridad
        </button>
      </div>
    </div>
  );
};

export default Paso6MatrizRiesgos;
