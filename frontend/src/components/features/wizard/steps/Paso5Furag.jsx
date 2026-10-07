const OPCIONES = [
  { value: 'SI', label: 'Sí' },
  { value: 'NO', label: 'No' },
  { value: 'NO_APLICA', label: 'No aplica' },
];

const normalizePreguntas = (preguntas) => {
  if (!Array.isArray(preguntas)) return [];

  return preguntas
    .map((p) => ({
      key: p?.key || p?.codigo || p?.id || '',
      label: p?.label || p?.pregunta || p?.nombre || '',
    }))
    .filter((p) => p.key && p.label);
};

const Paso5Furag = ({ data, onChange, errors, preguntas }) => {
  const furag = data.furag || {};
  const preguntasFurag = normalizePreguntas(preguntas);

  const handleRespuesta = (key, value) => {
    onChange({ furag: { ...furag, [key]: value } });
  };

  if (preguntasFurag.length === 0) {
    return (
      <div className="step-form">
        <h3 className="step-title">Cuestionario FURAG</h3>
        <p className="help-text">
          No se pudieron cargar las preguntas del catálogo FURAG. Verifique la configuración e intente nuevamente.
        </p>
      </div>
    );
  }

  return (
    <div className="step-form">
      <h3 className="step-title">Cuestionario FURAG</h3>
      <p className="help-text">
        Responda las preguntas obligatorias sobre la alineación del proyecto con el FURAG.
      </p>

      {preguntasFurag.map((pregunta) => (
        <div key={pregunta.key} className="furag-item">
          <p className="furag-pregunta">{pregunta.label} *</p>
          <div className="furag-opciones">
            {OPCIONES.map((opt) => (
              <label key={opt.value} className="radio-label">
                <input
                  type="radio"
                  name={`furag_${pregunta.key}`}
                  value={opt.value}
                  checked={furag[pregunta.key] === opt.value}
                  onChange={() => handleRespuesta(pregunta.key, opt.value)}
                />
                {opt.label}
              </label>
            ))}
          </div>
          {errors[`furag_${pregunta.key}`] && (
            <span className="error-text">{errors[`furag_${pregunta.key}`]}</span>
          )}
        </div>
      ))}
    </div>
  );
};

export default Paso5Furag;
