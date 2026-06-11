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

const normalizeOption = (option) => {
  if (!option) return null;
  if (typeof option === 'string') {
    return { value: option, label: option };
  }

  const value = option.value || option.codigo || option.key || '';
  const label = option.label || option.nombre || option.descripcion || value;
  return value ? { value, label } : null;
};

const ensureSelectedValue = (items, selectedValue) => {
  if (!selectedValue) return items;
  return items.some((item) => item.value === selectedValue)
    ? items
    : [...items, { value: selectedValue, label: selectedValue }];
};

const Paso4PetiComunicaciones = ({ data, onChange, errors, catalog, loadingCatalog = false }) => {
  const vigencias = Array.isArray(catalog?.vigencias) && catalog.vigencias.length > 0
    ? catalog.vigencias
    : VIGENCIAS_PETI;

  const estrategiasBase = Array.isArray(catalog?.estrategias) && catalog.estrategias.length > 0
    ? catalog.estrategias
    : ESTRATEGIAS_PETI;
  const estrategias = ensureSelectedValue(
    estrategiasBase.map(normalizeOption).filter(Boolean),
    data.estrategiaPeti
  );

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
              disabled={loadingCatalog}
            >
              <option value="">{loadingCatalog ? 'Cargando vigencias...' : 'Seleccione la vigencia'}</option>
              {vigencias.map((vigencia) => (
                <option key={vigencia} value={vigencia}>{vigencia}</option>
              ))}
            </select>
            {errors.vigenciaPeti && <span className="error-text">{errors.vigenciaPeti}</span>}
            <p className="help-text">Opciones administradas desde Configuración Seguridad &gt; Parámetros: peti_vigencias.</p>
          </div>

          <div className="form-group">
            <label className="form-label">Estrategia PETI *</label>
            <select
              className={`form-input ${errors.estrategiaPeti ? 'input-error' : ''}`}
              value={data.estrategiaPeti || ''}
              onChange={(e) => onChange({ estrategiaPeti: e.target.value })}
              disabled={loadingCatalog}
            >
              <option value="">{loadingCatalog ? 'Cargando estrategias...' : 'Seleccione la estrategia'}</option>
              {estrategias.map((estrategia) => (
                <option key={estrategia.value} value={estrategia.value}>{estrategia.label}</option>
              ))}
            </select>
            {errors.estrategiaPeti && <span className="error-text">{errors.estrategiaPeti}</span>}
            <p className="help-text">Opciones administradas desde Configuración Seguridad &gt; Parámetros: peti_estrategias.</p>
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
            Recuerde: debe cargar el documento del Plan de Comunicaciones en la gestión documental.
          </p>
        )}
      </div>
    </div>
  );
};

export default Paso4PetiComunicaciones;
