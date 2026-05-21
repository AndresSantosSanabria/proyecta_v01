import React from 'react';

const ROLES_EQUIPO = [
  'Analista de Sistemas',
  'Desarrollador Senior',
  'Desarrollador Junior',
  'Líder Técnico',
  'Arquitecto de Software',
  'Administrador de Base de Datos',
  'Ingeniero de Infraestructura',
  'Tester / QA',
  'Scrum Master',
  'Product Owner',
  'Analista de Seguridad',
  'Consultor Funcional',
];

const Paso2PatrocinadorEquipo = ({ data, onChange, errors }) => {
  const handlePatrocinador = (field, value) => {
    onChange({ patrocinador: { ...(data.patrocinador || {}), [field]: value } });
  };

  const handleIntegranteChange = (index, field, value) => {
    const nuevos = [...(data.equipoTrabajo || [])];
    nuevos[index] = { ...nuevos[index], [field]: value };
    onChange({ equipoTrabajo: nuevos });
  };

  const handleIntegranteAdd = () => {
    const nuevos = [...(data.equipoTrabajo || []), { nombre: '', cargo: '', rol: '' }];
    onChange({ equipoTrabajo: nuevos });
  };

  const handleIntegranteRemove = (index) => {
    const nuevos = (data.equipoTrabajo || []).filter((_, i) => i !== index);
    onChange({ equipoTrabajo: nuevos });
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Patrocinador del Proyecto</h3>

      <div className="form-grid">
        <div className="form-group">
          <label className="form-label">Nombre del Patrocinador *</label>
          <input
            className={`form-input ${errors.patrocinadorNombre ? 'input-error' : ''}`}
            value={data.patrocinador?.nombre || ''}
            onChange={(e) => handlePatrocinador('nombre', e.target.value)}
            placeholder="Nombre completo"
          />
          {errors.patrocinadorNombre && <span className="error-text">{errors.patrocinadorNombre}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Cargo *</label>
          <input
            className={`form-input ${errors.patrocinadorCargo ? 'input-error' : ''}`}
            value={data.patrocinador?.cargo || ''}
            onChange={(e) => handlePatrocinador('cargo', e.target.value)}
            placeholder="Ej: Secretario de TIC"
          />
          {errors.patrocinadorCargo && <span className="error-text">{errors.patrocinadorCargo}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Entidad *</label>
          <input
            className={`form-input ${errors.patrocinadorEntidad ? 'input-error' : ''}`}
            value={data.patrocinador?.entidad || ''}
            onChange={(e) => handlePatrocinador('entidad', e.target.value)}
            placeholder="Ej: Gobernación de Cundinamarca"
          />
          {errors.patrocinadorEntidad && <span className="error-text">{errors.patrocinadorEntidad}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Proceso SIG</label>
          <input
            className="form-input"
            value={data.patrocinador?.procesoSigc || ''}
            onChange={(e) => handlePatrocinador('procesoSigc', e.target.value)}
            placeholder="Código del proceso SIG"
          />
        </div>

        <div className="form-group">
          <label className="form-label">Procedimiento SIG</label>
          <input
            className="form-input"
            value={data.patrocinador?.procedimientoSigc || ''}
            onChange={(e) => handlePatrocinador('procedimientoSigc', e.target.value)}
            placeholder="Código del procedimiento SIG"
          />
        </div>
      </div>

      <h3 className="step-title" style={{ marginTop: '2rem' }}>Equipo TIC del Proyecto</h3>
      <p className="help-text">Registre los integrantes del equipo TIC que participarán en el proyecto.</p>

      {(data.equipoTrabajo || []).map((miembro, i) => (
        <div key={i} className="array-card">
          <div className="array-card-header">
            <strong>Integrante #{i + 1}</strong>
            <button type="button" className="btn-icon-danger" onClick={() => handleIntegranteRemove(i)}>✕</button>
          </div>
          <div className="form-grid array-card-body">
            <div className="form-group">
              <label className="form-label">Nombre</label>
              <input
                className="form-input"
                value={miembro.nombre || ''}
                onChange={(e) => handleIntegranteChange(i, 'nombre', e.target.value)}
                placeholder="Nombre del integrante"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Cargo</label>
              <input
                className="form-input"
                value={miembro.cargo || ''}
                onChange={(e) => handleIntegranteChange(i, 'cargo', e.target.value)}
                placeholder="Cargo en la entidad"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Rol en el Proyecto</label>
              <select
                className="form-input"
                value={miembro.rol || ''}
                onChange={(e) => handleIntegranteChange(i, 'rol', e.target.value)}
              >
                <option value="">Seleccione un rol</option>
                {ROLES_EQUIPO.map((rol) => (
                  <option key={rol} value={rol}>{rol}</option>
                ))}
              </select>
            </div>
          </div>
        </div>
      ))}

      <button type="button" className="btn-add" onClick={handleIntegranteAdd}>
        + Agregar integrante
      </button>
    </div>
  );
};

export default Paso2PatrocinadorEquipo;
