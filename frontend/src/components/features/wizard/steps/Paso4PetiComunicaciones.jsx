import { AutocompleteSelect } from '../../../common/AutocompleteSelect';

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
          ¿El proyecto pertenece al portafolio del Plan Estratégico de Tecnologías de la Información (PETI)? *
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
            <AutocompleteSelect
              value={data.vigenciaPeti || ''}
              onChange={(val) => onChange({ vigenciaPeti: val })}
              disabled={loadingCatalog}
              options={vigencias.map((v) => ({ value: v, label: v }))}
              placeholder={loadingCatalog ? 'Cargando vigencias...' : 'Seleccione la vigencia'}
              allLabel=""
              allValue=""
              className={errors.vigenciaPeti ? 'input-error' : ''}
            />
            {errors.vigenciaPeti && <span className="error-text">{errors.vigenciaPeti}</span>}
            <p className="help-text">Opciones administradas desde Configuracion Seguridad &gt; Listas: VIGENCIA_PETI.</p>
          </div>

          <div className="form-group">
            <label className="form-label">Estrategia PETI *</label>
            <AutocompleteSelect
              value={data.estrategiaPeti || ''}
              onChange={(val) => onChange({ estrategiaPeti: val })}
              disabled={loadingCatalog}
              options={estrategias.map((e) => ({ value: e.value, label: e.label }))}
              placeholder={loadingCatalog ? 'Cargando estrategias...' : 'Seleccione la estrategia'}
              allLabel=""
              allValue=""
              className={errors.estrategiaPeti ? 'input-error' : ''}
            />
            {errors.estrategiaPeti && <span className="error-text">{errors.estrategiaPeti}</span>}
            <p className="help-text">Opciones administradas desde Configuracion Seguridad &gt; Listas: ESTRATEGIA_PETI.</p>
          </div>
        </div>
      )}

      <div className="form-group">
        <label className="form-label" style={{ marginTop: '2rem', color: '#60a5fa', fontWeight: 600 }}>
          Plan de Comunicaciones
        </label>
        <p className="help-text">
          El Plan de Comunicaciones es un documento obligatorio. Deberá cargarlo en la gestión documental.
        </p>
      </div>
    </div>
  );
};

export default Paso4PetiComunicaciones;
