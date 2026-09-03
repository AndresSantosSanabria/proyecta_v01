import { useEffect, useRef, useState } from 'react';
import SpellCheckerTextarea from '../../../common/SpellCheckerTextarea';
import SpellCheckerInput from '../../../common/SpellCheckerInput';
import { AutocompleteSelect } from '../../../common/AutocompleteSelect';

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
          <AutocompleteSelect
            value={data.dependencia || ''}
            onChange={(val) => handleChange('dependencia', val)}
            options={DEPENDENCIAS.map((dep) => ({ value: dep, label: dep }))}
            placeholder="Seleccione una dependencia"
            allLabel=""
            allValue=""
            className={errors.dependencia ? 'input-error' : ''}
          />
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
          <AutocompleteSelect
            value={data.directorUsuarioId || ''}
            onChange={(val) => handleDirectorSelect(val)}
            disabled={directorsLoading || directorOptions.length === 0}
            options={directorOptions.map((d) => ({
              value: getDirectorId(d),
              label: `${getDirectorName(d)}${getDirectorRole(d) ? ` - ${getDirectorRole(d)}` : ''}`,
            }))}
            placeholder={directorsLoading ? 'Cargando directores...' : 'Seleccione un usuario director'}
            allLabel=""
            allValue=""
            className={errors.directorUsuarioId || errors.director ? 'input-error' : ''}
          />
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
