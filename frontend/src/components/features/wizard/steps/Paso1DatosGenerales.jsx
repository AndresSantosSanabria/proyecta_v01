import { useEffect, useRef, useState } from 'react';
import SpellCheckerTextarea from '../../../common/SpellCheckerTextarea';
import SpellCheckerInput from '../../../common/SpellCheckerInput';

const DEPENDENCIAS = [
  'Infraestructura',
  'Atención al Ciudadano',
  'Finanzas',
  'Prensa y Comunicaciones',
  'Seguridad de la Información',
  'Innovación y Tecnología',
  'Calidad de Software',
  'Planeación',
  'Jurídica',
  'Talento Humano',
];

const getDirectorId = (director) => director?.id ?? director?.usuarioId ?? director?.userId ?? '';
const getDirectorName = (director) => director?.nombre || director?.name || director?.username || '';
const getDirectorEmail = (director) => director?.correo || director?.email || '';
const getDirectorRole = (director) => director?.rolNombre || director?.rolCodigo || director?.rol || '';

const Paso1DatosGenerales = ({
  data,
  onChange,
  errors,
  directorOptions = [],
  directorsLoading = false,
  directorsError = '',
}) => {
  const [dependenciaOpen, setDependenciaOpen] = useState(false);
  const dependenciaRef = useRef(null);

  const handleChange = (field, value) => {
    onChange({ [field]: value });
  };

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (dependenciaRef.current && !dependenciaRef.current.contains(event.target)) {
        setDependenciaOpen(false);
      }
    };

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setDependenciaOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    window.addEventListener('keydown', handleEscape);

    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      window.removeEventListener('keydown', handleEscape);
    };
  }, []);

  const handleObjetivoAdd = () => {
    const nuevos = [...(data.objetivosEspecificos || []), ''];
    onChange({ objetivosEspecificos: nuevos });
  };

  const handleObjetivoChange = (index, value) => {
    const nuevos = [...(data.objetivosEspecificos || [])];
    nuevos[index] = value;
    onChange({ objetivosEspecificos: nuevos });
  };

  const handleObjetivoRemove = (index) => {
    const nuevos = (data.objetivosEspecificos || []).filter((_, i) => i !== index);
    onChange({ objetivosEspecificos: nuevos });
  };

  const handleDirectorSelect = (value) => {
    const selectedDirector = directorOptions.find((director) => String(getDirectorId(director)) === String(value));
    onChange({
      directorUsuarioId: value,
      director: selectedDirector ? getDirectorName(selectedDirector) : '',
      correoDirector: selectedDirector ? getDirectorEmail(selectedDirector) : '',
    });
  };

  return (
    <div className="step-form">
      <h3 className="step-title">Datos Generales del Proyecto</h3>

      <div className="form-grid">
        <div className="form-group">
          <label className="form-label">Nombre del Proyecto *</label>
          <SpellCheckerInput
            className={`form-input ${errors.nombre ? 'input-error' : ''}`}
            value={data.nombre || ''}
            onChange={(e) => handleChange('nombre', e.target.value)}
            placeholder="Ej: Modernización del Data Center Principal"
          />
          {errors.nombre && <span className="error-text">{errors.nombre}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Código del Proyecto</label>
          <input
            className="form-input form-input-muted"
            value={data.codigoPreview || 'IS-PROY-CUN-NNN (generado automáticamente)'}
            disabled
          />
          <span className="help-text">Se genera automáticamente según la normativa IS-PROY-CUN-NNN</span>
        </div>

        <div className="form-group">
          <label className="form-label">Dependencia Responsable *</label>
          <div className="custom-select" ref={dependenciaRef}>
            <button
              type="button"
              className={`form-input custom-select-trigger ${errors.dependencia ? 'input-error' : ''} ${dependenciaOpen ? 'open' : ''}`}
              onClick={() => setDependenciaOpen((current) => !current)}
              aria-haspopup="listbox"
              aria-expanded={dependenciaOpen}
            >
              <span className={`custom-select-value ${data.dependencia ? 'selected' : 'placeholder'}`}>
                {data.dependencia || 'Seleccione una dependencia'}
              </span>
              <span className={`custom-select-caret ${dependenciaOpen ? 'open' : ''}`} aria-hidden="true">⌄</span>
            </button>

            {dependenciaOpen && (
              <div className="custom-select-menu" role="listbox" aria-label="Dependencia Responsable">
                {DEPENDENCIAS.map((dep) => {
                  const selected = data.dependencia === dep;
                  return (
                    <button
                      key={dep}
                      type="button"
                      role="option"
                      aria-selected={selected}
                      className={`custom-select-option ${selected ? 'selected' : ''}`}
                      onClick={() => {
                        handleChange('dependencia', dep);
                        setDependenciaOpen(false);
                      }}
                    >
                      {dep}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
          {errors.dependencia && <span className="error-text">{errors.dependencia}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Fecha de Inicio *</label>
          <input
            type="date"
            className={`form-input ${errors.fechaInicio ? 'input-error' : ''}`}
            value={data.fechaInicio || ''}
            onChange={(e) => handleChange('fechaInicio', e.target.value)}
          />
          {errors.fechaInicio && <span className="error-text">{errors.fechaInicio}</span>}
        </div>

        <div className="form-group">
          <label className="form-label">Director TIC *</label>
          <select
            className={`form-input ${errors.directorUsuarioId || errors.director ? 'input-error' : ''}`}
            value={data.directorUsuarioId || ''}
            onChange={(e) => handleDirectorSelect(e.target.value)}
            disabled={directorsLoading || directorOptions.length === 0}
          >
            <option value="">
              {directorsLoading ? 'Cargando directores...' : 'Seleccione un usuario director'}
            </option>
            {directorOptions.map((director) => {
              const id = getDirectorId(director);
              const role = getDirectorRole(director);
              return (
                <option key={id || getDirectorEmail(director)} value={id}>
                  {getDirectorName(director)}{role ? ` - ${role}` : ''}
                </option>
              );
            })}
          </select>
          {directorsError && <span className="error-text">{directorsError}</span>}
          {!directorsLoading && !directorsError && directorOptions.length === 0 && (
            <span className="help-text">No hay usuarios con rol directivo disponibles para asignar.</span>
          )}
          {(errors.directorUsuarioId || errors.director) && (
            <span className="error-text">{errors.directorUsuarioId || errors.director}</span>
          )}
        </div>

        <div className="form-group">
          <label className="form-label">Director TIC - Correo Electrónico *</label>
          <input
            type="email"
            className={`form-input form-input-muted ${errors.correoDirector ? 'input-error' : ''}`}
            value={data.correoDirector || ''}
            readOnly
            placeholder="correo@cundinamarca.gov.co"
          />
          <span className="help-text">El correo se toma del usuario seleccionado.</span>
          {errors.correoDirector && <span className="error-text">{errors.correoDirector}</span>}
        </div>
      </div>

      <div className="form-group">
        <label className="form-label">Objetivo General del Proyecto *</label>
        <SpellCheckerTextarea
          className={`form-input form-textarea ${errors.objetivoGeneral ? 'input-error' : ''}`}
          value={data.objetivoGeneral || ''}
          onChange={(e) => handleChange('objetivoGeneral', e.target.value)}
          placeholder="Describa el objetivo principal del proyecto..."
          rows={4}
        />
        {errors.objetivoGeneral && <span className="error-text">{errors.objetivoGeneral}</span>}
      </div>

      <div className="form-group">
        <label className="form-label">Objetivos Específicos</label>
        {(data.objetivosEspecificos || []).map((obj, i) => (
          <div key={i} className="array-field-row">
            <SpellCheckerInput
              className="form-input"
              value={obj}
              onChange={(e) => handleObjetivoChange(i, e.target.value)}
              placeholder={`Objetivo específico ${i + 1}`}
            />
            <button type="button" className="btn-icon-danger" onClick={() => handleObjetivoRemove(i)}>
              ✕
            </button>
          </div>
        ))}
        <button type="button" className="btn-add" onClick={handleObjetivoAdd}>
          + Agregar objetivo específico
        </button>
      </div>
    </div>
  );
};

export default Paso1DatosGenerales;
