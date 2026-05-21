import React from 'react';

const PREGUNTAS_FURAG = [
  { key: 'infraestructuraDatos', label: '¿El proyecto incluye uso de infraestructura de datos (datos abiertos, big data, analytics)?' },
  { key: 'interoperabilidad', label: '¿El proyecto requiere interoperabilidad con otros sistemas de la entidad o del Estado?' },
  { key: 'digitalizacionAutomatizacion', label: '¿El proyecto contempla digitalización o automatización de procesos?' },
  { key: 'contratacionPublica', label: '¿El proyecto está relacionado con contratación pública electrónica?' },
  { key: 'serviciosNube', label: '¿El proyecto utilizará servicios en la nube (IaaS, PaaS, SaaS)?' },
  { key: 'sandbox', label: '¿El proyecto requiere un entorno Sandbox regulatorio para pruebas?' },
  { key: 'tecnologiasEmergentes', label: '¿El proyecto hace uso de tecnologías emergentes (IA, Blockchain, IoT)?' },
];

const OPCIONES = [
  { value: 'SI', label: 'Sí' },
  { value: 'NO', label: 'No' },
  { value: 'NA', label: 'No aplica' },
];

const Paso5Furag = ({ data, onChange, errors }) => {
  const furag = data.furag || {};

  const handleRespuesta = (key, value) => {
    onChange({ furag: { ...furag, [key]: value } });
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Cuestionario FURAG</h3>
      <p className="help-text">
        Responda las siguientes preguntas obligatorias sobre la alineación del proyecto con el Formulario Único de Reporte de Avance de la Gestión (FURAG).
      </p>

      {PREGUNTAS_FURAG.map((pregunta) => (
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
