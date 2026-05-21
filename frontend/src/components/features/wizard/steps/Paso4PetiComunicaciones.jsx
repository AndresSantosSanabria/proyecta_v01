import React from 'react';

const VIGENCIAS_PETI = [
  '2020-2024',
  '2024-2027',
  '2027-2030',
];

const ESTRATEGIAS_PETI = [
  { value: 'TECNOLOGIAS_INFORMACION', label: 'Tecnologías de la Información' },
  { value: 'TRANSFORMACION_DIGITAL', label: 'Transformación Digital' },
  { value: 'CIUDADES_TERRITORIOS_INTELIGENTES', label: 'Ciudades y Territorios Inteligentes' },
  { value: 'GOBIERNO_DIGITAL', label: 'Gobierno Digital' },
];

const Paso4PetiComunicaciones = ({ data, onChange, errors }) => {
  return (
    <div className="step-form">
      <h3 className="step-title">Plan Estratégico de Tecnologías de la Información (PETI)</h3>

      <div className="form-group">
        <label className="form-label">
          ¿El proyecto está definido en el Plan Estratégico de Tecnologías de la Información (PETI)? *
        </label>
        <div className="radio-group">
          <label className="radio-label">
            <input
              type="radio"
              name="peti"
              checked={data.peti === true}
              onChange={() => onChange({ peti: true })}
            />
            Sí
          </label>
          <label className="radio-label">
            <input
              type="radio"
              name="peti"
              checked={data.peti === false}
              onChange={() => onChange({ peti: false, vigenciaPeti: '', estrategiaPeti: null })}
            />
            No
          </label>
        </div>
      </div>

      {data.peti === true && (
        <div className="form-grid">
          <div className="form-group">
            <label className="form-label">Vigencia PETI *</label>
            <select
              className={`form-input ${errors.vigenciaPeti ? 'input-error' : ''}`}
              value={data.vigenciaPeti || ''}
              onChange={(e) => onChange({ vigenciaPeti: e.target.value })}
            >
              <option value="">Seleccione la vigencia</option>
              {VIGENCIAS_PETI.map((v) => (
                <option key={v} value={v}>{v}</option>
              ))}
            </select>
            {errors.vigenciaPeti && <span className="error-text">{errors.vigenciaPeti}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Estrategia PETI *</label>
            <select
              className={`form-input ${errors.estrategiaPeti ? 'input-error' : ''}`}
              value={data.estrategiaPeti || ''}
              onChange={(e) => onChange({ estrategiaPeti: e.target.value })}
            >
              <option value="">Seleccione la estrategia</option>
              {ESTRATEGIAS_PETI.map((est) => (
                <option key={est.value} value={est.value}>{est.label}</option>
              ))}
            </select>
            {errors.estrategiaPeti && <span className="error-text">{errors.estrategiaPeti}</span>}
          </div>
        </div>
      )}

      <h3 className="step-title" style={{ marginTop: '2rem' }}>Plan de Comunicaciones</h3>

      <div className="form-group">
        <label className="form-label">
          ¿El proyecto cuenta con un Plan de Comunicaciones? *
        </label>
        <div className="radio-group">
          <label className="radio-label">
            <input
              type="radio"
              name="tienePlanComunicaciones"
              checked={data.tienePlanComunicaciones === true}
              onChange={() => onChange({ tienePlanComunicaciones: true })}
            />
            Sí
          </label>
          <label className="radio-label">
            <input
              type="radio"
              name="tienePlanComunicaciones"
              checked={data.tienePlanComunicaciones === false}
              onChange={() => onChange({ tienePlanComunicaciones: false })}
            />
            No
          </label>
        </div>
        {data.tienePlanComunicaciones === true && (
          <p className="help-text" style={{ marginTop: '0.5rem', color: '#d97706' }}>
            Recuerde: Deberá cargar el documento del Plan de Comunicaciones en el Paso 6 (Gestión Documental).
          </p>
        )}
      </div>
    </div>
  );
};

export default Paso4PetiComunicaciones;
