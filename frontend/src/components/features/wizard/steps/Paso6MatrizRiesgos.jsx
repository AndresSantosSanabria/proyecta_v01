import { Plus, Trash2, ShieldAlert, AlertCircle } from 'lucide-react';
import './Paso6MatrizRiesgos.css';

const PROBABILIDADES = [
  { value: 'BAJA', label: 'Baja' },
  { value: 'MEDIA', label: 'Media' },
  { value: 'ALTA', label: 'Alta' },
];

const IMPACTOS = [
  { value: 'BAJO', label: 'Bajo' },
  { value: 'MEDIO', label: 'Medio' },
  { value: 'ALTO', label: 'Alto' },
];

const emptyRiesgo = () => ({
  descripcion: '',
  probabilidad: 'MEDIA',
  impacto: 'MEDIO',
  tratamiento: '',
  entidadResponsable: '',
  accionesMitigacion: '',
  fechaAccion: '',
});

const Paso6MatrizRiesgos = ({ data, onChange, errors }) => {
  const riesgos = data.riesgosIniciales || [];

  const handleChange = (index, field, value) => {
    const next = [...riesgos];
    next[index] = { ...next[index], [field]: value };
    onChange({ riesgosIniciales: next });
  };

  const handleAdd = () => {
    onChange({ riesgosIniciales: [...riesgos, emptyRiesgo()] });
  };

  const handleRemove = (index) => {
    if (riesgos.length <= 2) return;
    onChange({ riesgosIniciales: riesgos.filter((_, i) => i !== index) });
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Matriz de Riesgos</h3>
      <p className="help-text">
        Registre al menos 2 riesgos identificados para el proyecto. Estos quedarán pendientes de tratamiento y podrán gestionarse una vez completado el proyecto.
      </p>

      {errors.riesgosIniciales && (
        <span className="error-text">{errors.riesgosIniciales}</span>
      )}

      <div className="riesgo-list">
        {riesgos.map((riesgo, index) => (
          <div key={index} className="riesgo-card">
            <div className="riesgo-card-header">
              <div className="riesgo-card-icon">
                <ShieldAlert size={17} strokeWidth={2} />
              </div>
              <div className="riesgo-card-title">
                <h4>
                  Riesgo {index + 1}
                  <span className="riesgo-required">Requerido</span>
                </h4>
              </div>
              {riesgos.length > 2 && (
                <button
                  type="button"
                  className="riesgo-card-remove"
                  onClick={() => handleRemove(index)}
                  aria-label={`Eliminar riesgo ${index + 1}`}
                >
                  <Trash2 size={14} strokeWidth={2.5} />
                </button>
              )}
            </div>

            <div className="riesgo-card-body">
              <div className="riesgo-field">
                <label className="form-label">Descripción del riesgo *</label>
                <textarea
                  className={`form-input form-textarea ${errors[`riesgo_${index}_descripcion`] ? 'input-error' : ''}`}
                  value={riesgo.descripcion}
                  onChange={(e) => handleChange(index, 'descripcion', e.target.value)}
                  rows={3}
                  placeholder="Describa el riesgo identificado"
                />
                {errors[`riesgo_${index}_descripcion`] && (
                  <span className="error-text">{errors[`riesgo_${index}_descripcion`]}</span>
                )}
              </div>

              <div className="riesgo-field-row">
                <div className="riesgo-field">
                  <label className="form-label">Probabilidad *</label>
                  <select
                    className={`form-input ${errors[`riesgo_${index}_probabilidad`] ? 'input-error' : ''}`}
                    value={riesgo.probabilidad}
                    onChange={(e) => handleChange(index, 'probabilidad', e.target.value)}
                  >
                    {PROBABILIDADES.map((p) => (
                      <option key={p.value} value={p.value}>{p.label}</option>
                    ))}
                  </select>
                  {errors[`riesgo_${index}_probabilidad`] && (
                    <span className="error-text">{errors[`riesgo_${index}_probabilidad`]}</span>
                  )}
                </div>

                <div className="riesgo-field">
                  <label className="form-label">Impacto *</label>
                  <select
                    className={`form-input ${errors[`riesgo_${index}_impacto`] ? 'input-error' : ''}`}
                    value={riesgo.impacto}
                    onChange={(e) => handleChange(index, 'impacto', e.target.value)}
                  >
                    {IMPACTOS.map((i) => (
                      <option key={i.value} value={i.value}>{i.label}</option>
                    ))}
                  </select>
                  {errors[`riesgo_${index}_impacto`] && (
                    <span className="error-text">{errors[`riesgo_${index}_impacto`]}</span>
                  )}
                </div>
              </div>

              <div className="riesgo-field">
                <label className="form-label">Cómo mitigar el riesgo</label>
                <textarea
                  className="form-input form-textarea"
                  value={riesgo.tratamiento}
                  onChange={(e) => handleChange(index, 'tratamiento', e.target.value)}
                  rows={2}
                  placeholder="Plan de tratamiento: aceptar, mitigar, transferir, evitar"
                />
              </div>

              <div className="riesgo-field-row">
                <div className="riesgo-field">
                  <label className="form-label">Responsable</label>
                  <input
                    type="text"
                    className="form-input"
                    value={riesgo.entidadResponsable}
                    onChange={(e) => handleChange(index, 'entidadResponsable', e.target.value)}
                    placeholder="Entidad o persona responsable"
                  />
                </div>

                <div className="riesgo-field">
                  <label className="form-label">Fecha de acción</label>
                  <input
                    type="date"
                    className="form-input"
                    value={riesgo.fechaAccion}
                    onChange={(e) => handleChange(index, 'fechaAccion', e.target.value)}
                  />
                </div>
              </div>

              <div className="riesgo-field">
                <label className="form-label">Acciones de mitigación</label>
                <textarea
                  className="form-input form-textarea"
                  value={riesgo.accionesMitigacion}
                  onChange={(e) => handleChange(index, 'accionesMitigacion', e.target.value)}
                  rows={2}
                  placeholder="Acciones concretas realizadas para mitigar el riesgo"
                />
              </div>
            </div>
          </div>
        ))}
      </div>

      <button type="button" className="btn-add-riesgo" onClick={handleAdd}>
        <Plus size={16} strokeWidth={2.5} />
        Agregar riesgo
      </button>
    </div>
  );
};

export default Paso6MatrizRiesgos;
