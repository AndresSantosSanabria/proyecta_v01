import { useEffect, useState } from 'react';
import configCatalogService from '../../../../services/configCatalogService';
import projectService from '../../../../services/projectService';
import SpellCheckerTextarea from '../../../common/SpellCheckerTextarea';
import SpellCheckerInput from '../../../common/SpellCheckerInput';

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
    onChange({ equipoTrabajo: [...(data.equipoTrabajo || []), { nombre: '', cargo: '', rol: '', dependencia: '', telefono: '', correo: '' }] });
  };

  const handleIntegranteRemove = (index) => {
    onChange({ equipoTrabajo: (data.equipoTrabajo || []).filter((_, i) => i !== index) });
  };

  const handleStakeholderChange = (index, field, value) => {
    const nuevos = [...(data.stakeholders || [])];
    nuevos[index] = { ...nuevos[index], [field]: value };
    onChange({ stakeholders: nuevos });
  };

  const handleStakeholderAdd = () => {
    onChange({ stakeholders: [...(data.stakeholders || []), { rol: '', descripcion: '', interes: '', impacto: '' }] });
  };

  const handleStakeholderRemove = (index) => {
    onChange({ stakeholders: (data.stakeholders || []).filter((_, i) => i !== index) });
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
          <SpellCheckerInput
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
          <SpellCheckerInput
            className={`form-input ${errors.patrocinadorCargo ? 'input-error' : ''}`}
            value={data.patrocinador?.cargo || ''}
            onChange={(e) => handlePatrocinador('cargo', e.target.value)}
            placeholder="Ej: Secretario de TIC"
          />
          {errors.patrocinadorCargo && <span className="error-text">{errors.patrocinadorCargo}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Proceso SIGC</label>
          <SpellCheckerInput
            className="form-input"
            value={data.patrocinador?.procesoSigc || ''}
            onChange={(e) => handlePatrocinador('procesoSigc', e.target.value)}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Procedimiento SIGC</label>
          <SpellCheckerInput
            className="form-input"
            value={data.patrocinador?.procedimientoSigc || ''}
            onChange={(e) => handlePatrocinador('procedimientoSigc', e.target.value)}
          />
        </div>
      </div>

      <h3 className="step-title" style={{ marginTop: '2rem' }}>Equipo de Trabajo</h3>
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
              <SpellCheckerInput
                className="form-input"
                value={miembro.nombre || ''}
                onChange={(e) => handleIntegranteChange(i, 'nombre', e.target.value)}
                placeholder="Nombre del integrante"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Cargo</label>
              <SpellCheckerInput
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
            <div className="form-group">
              <label className="form-label">Dependencia</label>
              <SpellCheckerInput
                className="form-input"
                value={miembro.dependencia || ''}
                onChange={(e) => handleIntegranteChange(i, 'dependencia', e.target.value)}
                placeholder="Dependencia o área"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Teléfono</label>
              <input
                className="form-input"
                inputMode="numeric"
                pattern="[0-9]*"
                value={miembro.telefono || ''}
                onChange={(e) => {
                  const soloNumeros = e.target.value.replace(/[^0-9]/g, '');
                  handleIntegranteChange(i, 'telefono', soloNumeros);
                }}
                placeholder="Solo números"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Correo electrónico</label>
              <input
                className="form-input"
                type="email"
                pattern="[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"
                value={miembro.correo || ''}
                onChange={(e) => handleIntegranteChange(i, 'correo', e.target.value)}
                placeholder="correo@ejemplo.com"
              />
            </div>
          </div>
        </div>
      ))}

      <button type="button" className="btn-add" onClick={handleIntegranteAdd}>
        + Agregar integrante
      </button>

      <h3 className="step-title" style={{ marginTop: '2rem' }}>Grupo de Interes (Stakeholders)</h3>
      <p className="help-text">Identifique los grupos de interes del proyecto y su nivel de impacto.</p>

      {(data.stakeholders || []).map((stakeholder, i) => (
        <div key={i} className="array-card">
          <div className="array-card-header">
            <strong>Stakeholder #{i + 1}</strong>
            <button type="button" className="btn-icon-danger" onClick={() => handleStakeholderRemove(i)}>✕</button>
          </div>
          <div className="form-grid array-card-body">
            <div className="form-group">
              <label className="form-label">Rol</label>
              <SpellCheckerInput
                className="form-input"
                value={stakeholder.rol || ''}
                onChange={(e) => handleStakeholderChange(i, 'rol', e.target.value)}
                placeholder="Ej: Gestores de cuentas de cobro"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Descripcion</label>
              <SpellCheckerInput
                className="form-input"
                value={stakeholder.descripcion || ''}
                onChange={(e) => handleStakeholderChange(i, 'descripcion', e.target.value)}
                placeholder="Descripcion del grupo de interes"
              />
            </div>
            <div className="form-group">
              <label className="form-label">Interes / Expectativas</label>
              <SpellCheckerTextarea
                className="form-input form-textarea"
                value={stakeholder.interes || ''}
                onChange={(e) => handleStakeholderChange(i, 'interes', e.target.value)}
                placeholder="Que espera obtener del proyecto"
                rows={3}
              />
            </div>
            <div className="form-group">
              <label className="form-label">Impacto en el Proyecto</label>
              <SpellCheckerTextarea
                className="form-input form-textarea"
                value={stakeholder.impacto || ''}
                onChange={(e) => handleStakeholderChange(i, 'impacto', e.target.value)}
                placeholder="Como impacta en el proyecto"
                rows={3}
              />
            </div>
          </div>
        </div>
      ))}

      <button type="button" className="btn-add" onClick={handleStakeholderAdd}>
        + Agregar stakeholder
      </button>
    </div>
  );
};

export default Paso2PatrocinadorEquipo;
