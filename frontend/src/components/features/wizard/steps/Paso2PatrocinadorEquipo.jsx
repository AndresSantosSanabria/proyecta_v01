import { useEffect, useState } from 'react';
import configCatalogService from '../../../../services/configCatalogService';
import projectService from '../../../../services/projectService';

const Paso2PatrocinadorEquipo = ({ data, onChange, errors }) => {
  const [rolesEquipo, setRolesEquipo] = useState([]);
  const [patrocinadores, setPatrocinadores] = useState([]);

  useEffect(() => {
    const loadRoles = async () => {
      try {
        const values = await configCatalogService.listarValoresParametrica('ROL_EQUIPO');
        if (Array.isArray(values) && values.length > 0) setRolesEquipo(values);
      } catch {
        setRolesEquipo([]);
      }
    };
    const loadPatrocinadores = async () => {
      try {
        const list = await projectService.getPatrocinadores();
        setPatrocinadores(list);
      } catch {
        setPatrocinadores([]);
      }
    };
    loadRoles();
    loadPatrocinadores();
  }, []);

  const handlePatrocinador = (field, value) => {
    onChange({ patrocinador: { ...(data.patrocinador || {}), [field]: value } });
  };

  const handleNombrePatrocinador = (value) => {
    const encontrado = patrocinadores.find(
      (p) => p.nombre?.toLowerCase() === value.toLowerCase()
    );
    if (encontrado) {
      onChange({
        patrocinador: {
          nombre: encontrado.nombre,
          cargo: encontrado.cargo || '',
          entidad: encontrado.entidad || '',
          procesoSigc: encontrado.procesoSigc || '',
          procedimientoSigc: encontrado.procedimientoSigc || '',
        },
      });
    } else {
      handlePatrocinador('nombre', value);
    }
  };

  const handleIntegranteChange = (index, field, value) => {
    const nuevos = [...(data.equipoTrabajo || [])];
    nuevos[index] = { ...nuevos[index], [field]: value };
    onChange({ equipoTrabajo: nuevos });
  };

  const handleIntegranteAdd = () => {
    onChange({ equipoTrabajo: [...(data.equipoTrabajo || []), { nombre: '', cargo: '', rol: '' }] });
  };

  const handleIntegranteRemove = (index) => {
    onChange({ equipoTrabajo: (data.equipoTrabajo || []).filter((_, i) => i !== index) });
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Patrocinador del Proyecto</h3>

      <datalist id="patrocinadores-list">
        {patrocinadores.map((p, i) => (
          <option key={i} value={p.nombre} />
        ))}
      </datalist>

      <div className="form-grid">
        <div className="form-group">
          <label className="form-label">Nombre del Patrocinador *</label>
          <input
            className={`form-input ${errors.patrocinadorNombre ? 'input-error' : ''}`}
            value={data.patrocinador?.nombre || ''}
            onChange={(e) => handleNombrePatrocinador(e.target.value)}
            placeholder="Nombre completo"
            list="patrocinadores-list"
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
          <label className="form-label">Proceso SIGC</label>
          <input
            className="form-input"
            value={data.patrocinador?.procesoSigc || ''}
            onChange={(e) => handlePatrocinador('procesoSigc', e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Procedimiento SIGC</label>
          <input
            className="form-input"
            value={data.patrocinador?.procedimientoSigc || ''}
            onChange={(e) => handlePatrocinador('procedimientoSigc', e.target.value)}
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
                {rolesEquipo.map((rol) => (
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
