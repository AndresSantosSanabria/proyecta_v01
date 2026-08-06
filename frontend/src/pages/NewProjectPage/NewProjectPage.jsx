import { useEffect, useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCircle, Info, X } from 'lucide-react';
import projectService from '../../services/projectService';
import { usePermission } from '../../hooks/usePermission';
import './NewProjectPage.css';

const INITIAL_STATE = {
  codigoProyecto: '',
  nombre: '',
  objetivoGeneral: '',
  directorUsuarioId: '',
  director: '',
  correoDirector: '',
};

const getDirectorId = (director) => director?.id ?? director?.usuarioId ?? director?.userId ?? '';
const getDirectorName = (director) => director?.nombre || director?.name || director?.username || '';
const getDirectorEmail = (director) => director?.correo || director?.email || '';
const getDirectorRole = (director) => director?.rolNombre || director?.rolCodigo || director?.rol || '';

const normalizeProjectCode = (value) => String(value || '').trim().toUpperCase();

const extractApiMessage = (error) => (
  error?.response?.data?.detail
  || error?.response?.data?.message
  || error?.response?.data?.title
  || error?.message
  || 'No se pudo registrar el proyecto.'
);

const NewProjectPage = () => {
  const navigate = useNavigate();
  const canCreateProject = usePermission('PROYECTO:CREAR');
  const [formData, setFormData] = useState(INITIAL_STATE);
  const [errors, setErrors] = useState({});
  const [directorOptions, setDirectorOptions] = useState([]);
  const [directorsLoading, setDirectorsLoading] = useState(true);
  const [directorsError, setDirectorsError] = useState('');
  const [codeLoading, setCodeLoading] = useState(true);
  const [codeError, setCodeError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState(false);

  const selectedDirector = useMemo(
    () => directorOptions.find((director) => String(getDirectorId(director)) === String(formData.directorUsuarioId)) || null,
    [directorOptions, formData.directorUsuarioId],
  );

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, []);

  useEffect(() => {
    if (!canCreateProject) return undefined;

    let mounted = true;

    projectService.getAssignableDirectors()
      .then((directors) => {
        if (!mounted) return;
        setDirectorOptions(Array.isArray(directors) ? directors : []);
      })
      .catch((error) => {
        if (!mounted) return;
        console.error('No se pudieron cargar los directores asignables:', error);
        setDirectorOptions([]);
        setDirectorsError('No fue posible cargar los usuarios directores disponibles.');
      })
      .finally(() => {
        if (mounted) {
          setDirectorsLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [canCreateProject]);

  useEffect(() => {
    if (!canCreateProject) return undefined;

    let mounted = true;

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setCodeLoading(true);
    setCodeError('');

    projectService.getSiguienteCodigo()
      .then((codigo) => {
        if (!mounted) return;
        setFormData((current) => ({
          ...current,
          codigoProyecto: codigo || '',
        }));
      })
      .catch((error) => {
        if (!mounted) return;
        console.error('No se pudo generar el codigo automatico del proyecto:', error);
        setCodeError('No fue posible previsualizar el codigo. El servidor lo generara al guardar.');
      })
      .finally(() => {
        if (mounted) {
          setCodeLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [canCreateProject]);

  useEffect(() => {
    if (!submitSuccess) return undefined;

    const timer = window.setTimeout(() => {
      navigate('/projects');
    }, 1200);

    return () => window.clearTimeout(timer);
  }, [navigate, submitSuccess]);

  const updateField = (field, value) => {
    setFormData((current) => ({ ...current, [field]: value }));
    setErrors((current) => {
      if (!current[field]) return current;
      const next = { ...current };
      delete next[field];
      return next;
    });
  };

  const handleDirectorChange = (value) => {
    const director = directorOptions.find((item) => String(getDirectorId(item)) === String(value));
    setFormData((current) => ({
      ...current,
      directorUsuarioId: value,
      director: director ? getDirectorName(director) : '',
      correoDirector: director ? getDirectorEmail(director) : '',
    }));
    setErrors((current) => {
      const next = { ...current };
      delete next.directorUsuarioId;
      delete next.director;
      delete next.correoDirector;
      return next;
    });
  };

  const validate = () => {
    const nextErrors = {};
    const codigoProyecto = normalizeProjectCode(formData.codigoProyecto);

    if (codeLoading) {
      nextErrors.codigoProyecto = 'Espere a que se genere el codigo automatico.';
    } else if (!codigoProyecto && !codeError) {
      nextErrors.codigoProyecto = 'El codigo automatico del proyecto es obligatorio.';
    } else if (codigoProyecto && !/^[A-Z0-9][A-Z0-9_-]*$/.test(codigoProyecto)) {
      nextErrors.codigoProyecto = 'Use solo letras, numeros, guiones y guiones bajos.';
    }

    if (!formData.nombre.trim()) {
      nextErrors.nombre = 'El nombre del proyecto es obligatorio.';
    }

    if (!formData.objetivoGeneral.trim()) {
      nextErrors.objetivoGeneral = 'El objetivo del proyecto es obligatorio.';
    }

    if (!formData.directorUsuarioId) {
      nextErrors.directorUsuarioId = 'Seleccione un Director de Proyecto existente.';
    }

    setErrors(nextErrors);
    return nextErrors;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) return;

    setSubmitting(true);
    setSubmitError('');

    try {
      const codigoProyecto = normalizeProjectCode(formData.codigoProyecto);
      const payload = {
        nombre: formData.nombre.trim(),
        objetivoGeneral: formData.objetivoGeneral.trim(),
        directorUsuarioId: Number(formData.directorUsuarioId),
      };
      if (codigoProyecto) {
        payload.codigoProyecto = codigoProyecto;
      }
      await projectService.registerInitial(payload);
      setSubmitSuccess(true);
    } catch (error) {
      setSubmitError(extractApiMessage(error));
    } finally {
      setSubmitting(false);
    }
  };

  if (!canCreateProject) {
    return <Navigate to="/" replace />;
  }

  if (submitSuccess) {
    return (
      <div className="new-project-page">
        <div className="success-container">
          <CheckCircle size={64} color="#16a34a" />
          <h2>Proyecto registrado inicialmente</h2>
          <p>Queda pendiente de completar por el Director asignado en su primer ingreso.</p>
        </div>
      </div>
    );
  }

  return (
    <div
      className="new-project-overlay"
      onClick={(event) => {
        if (event.target === event.currentTarget) navigate(-1);
      }}
    >
      <div
        className="new-project-modal"
        onClick={(event) => event.stopPropagation()}
        onMouseDown={(event) => event.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-project-title"
      >
        <div className="new-project-modal-header">
          <div className="modal-header-copy">
            <button className="btn-back btn-back-modal" onClick={() => navigate(-1)} type="button">
              <ArrowLeft size={18} />
              <span>Volver</span>
            </button>
            <div>
              <h1 id="new-project-title">Registro Inicial del Proyecto</h1>
              <p>Solo registre los datos base. La informacion restante la completara el Director asignado.</p>
            </div>
          </div>

          <button className="modal-close-btn" onClick={() => navigate(-1)} type="button" aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>

        <form className="new-project-modal-body initial-registration-body" onSubmit={handleSubmit}>
          <aside className="initial-registration-aside" aria-label="Flujo de registro inicial">
            <span className="modal-flow-badge">Momento 1 - Gestor de Proyectos</span>
            <h2>Registro inicial controlado</h2>
            <p>
              Este formulario solo crea la ficha base del proyecto. El sistema bloquea la operacion normal hasta que
              el Director asignado complete la planeacion en su primer ingreso.
            </p>

            <div className="initial-flow-list">
              <article className="initial-flow-item active">
                <span>01</span>
                <div>
                  <strong>Guardar datos base</strong>
                  <small>Codigo, nombre, objetivo y Director asignado.</small>
                </div>
              </article>
              <article className="initial-flow-item">
                <span>02</span>
                <div>
                  <strong>Estado pendiente</strong>
                  <small>El proyecto queda en Pendiente de Completar.</small>
                </div>
              </article>
              <article className="initial-flow-item">
                <span>03</span>
                <div>
                  <strong>Primera apertura</strong>
                  <small>El Director completa la informacion operativa.</small>
                </div>
              </article>
            </div>

            <div className="locked-next-card">
              <span>Información que queda para el Director</span>
              <div className="locked-next-grid">
                <small>Fechas</small>
                <small>Alcance</small>
                <small>Presupuesto</small>
                <small>Fases y entregables</small>
                <small>PETI</small>
                <small>FURAG</small>
              </div>
            </div>
          </aside>

          <section className="initial-registration-form-card">
            <div className="info-alert">
              <Info size={18} className="info-icon" />
              <p>
                El proyecto quedara en estado Pendiente de Completar. El Director asignado no podra usar los modulos
                operativos hasta completar la informacion restante en su primer ingreso.
              </p>
            </div>

            <div className="step-form">
              <div className="form-card-heading">
                <span>Datos obligatorios</span>
                <h3 className="step-title">Información permitida en creación</h3>
                <p>Estos campos quedan protegidos para el Director y no se editan en el asistente de completitud.</p>
              </div>

              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label">Codigo del Proyecto</label>
                  <input
                    className={`form-input form-input-muted ${errors.codigoProyecto ? 'input-error' : ''}`}
                    value={codeLoading && !formData.codigoProyecto ? 'Generando codigo...' : (formData.codigoProyecto || 'Se generara al guardar')}
                    readOnly
                    disabled={codeLoading}
                    placeholder="IS-PROY-CUN-YYYY-NNN"
                  />
                  <span className="help-text">
                    Se genera automaticamente segun el consecutivo existente.
                  </span>
                  {errors.codigoProyecto && <span className="error-text">{errors.codigoProyecto}</span>}
                  {codeError && <span className="help-text">{codeError}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label">Nombre del Proyecto *</label>
                  <input
                    className={`form-input ${errors.nombre ? 'input-error' : ''}`}
                    value={formData.nombre}
                    onChange={(event) => updateField('nombre', event.target.value)}
                    placeholder="Ej: Modernizacion del Data Center"
                  />
                  {errors.nombre && <span className="error-text">{errors.nombre}</span>}
                </div>

                <div className="form-group">
                  <label className="form-label">Director de Proyecto Asignado *</label>
                  <select
                    className={`form-input ${errors.directorUsuarioId ? 'input-error' : ''}`}
                    value={formData.directorUsuarioId}
                    onChange={(event) => handleDirectorChange(event.target.value)}
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
                  {errors.directorUsuarioId && <span className="error-text">{errors.directorUsuarioId}</span>}
                  {directorsError && <span className="error-text">{directorsError}</span>}
                  {!directorsLoading && !directorsError && directorOptions.length === 0 && (
                    <span className="help-text">No hay usuarios directores activos disponibles.</span>
                  )}
                </div>

                <div className="form-group">
                  <label className="form-label">Correo del Director</label>
                  <input
                    className="form-input form-input-muted"
                    value={selectedDirector ? getDirectorEmail(selectedDirector) : formData.correoDirector}
                    readOnly
                    placeholder="Se completa automaticamente"
                  />
                  <span className="help-text">El correo proviene del usuario seleccionado.</span>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Objetivo del Proyecto *</label>
                <textarea
                  className={`form-input form-textarea ${errors.objetivoGeneral ? 'input-error' : ''}`}
                  value={formData.objetivoGeneral}
                  onChange={(event) => updateField('objetivoGeneral', event.target.value)}
                  placeholder="Describa el objetivo principal del proyecto..."
                  rows={5}
                />
                {errors.objetivoGeneral && <span className="error-text">{errors.objetivoGeneral}</span>}
              </div>
            </div>

            {submitError && (
              <div className="error-banner">
                <AlertTriangle size={18} />
                <span>{submitError}</span>
              </div>
            )}

            <div className="wizard-actions">
              <span />
              <div className="wizard-actions-right">
                <button type="button" className="btn-secondary" onClick={() => navigate(-1)} disabled={submitting}>
                  Cancelar
                </button>
                <button className="btn-primary btn-submit" type="submit" disabled={submitting || directorsLoading || codeLoading}>
                  {submitting ? 'Registrando...' : 'Guardar y Continuar'}
                  {!submitting && <ArrowLeft size={16} style={{ transform: 'rotate(180deg)' }} />}
                </button>
              </div>
            </div>
          </section>
        </form>
      </div>
    </div>
  );
};

export default NewProjectPage;
