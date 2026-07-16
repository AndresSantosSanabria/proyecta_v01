import { useEffect, useMemo, useState, useCallback } from 'react';
import { AlertTriangle, ArrowLeft, ArrowRight, LockKeyhole, Save } from 'lucide-react';
import Paso2PatrocinadorEquipo from '../features/wizard/steps/Paso2PatrocinadorEquipo';
import Paso3FasesHitosEntregables from '../features/wizard/steps/Paso3FasesHitosEntregables';
import Paso4PetiComunicaciones from '../features/wizard/steps/Paso4PetiComunicaciones';
import Paso5Furag from '../features/wizard/steps/Paso5Furag';
import Paso6GestionDocumental from '../features/wizard/steps/Paso6GestionDocumental';
import configCatalogService from '../../services/configCatalogService';
import '../../pages/NewProjectPage/NewProjectPage.css';
import './ProjectOnboardingWizard.css';

const DEPENDENCIAS = [
  'Infraestructura',
  'Atencion al Ciudadano',
  'Finanzas',
  'Prensa y Comunicaciones',
  'Seguridad de la Informacion',
  'Innovacion y Tecnologia',
  'Calidad de Software',
  'Planeacion',
  'Juridica',
  'Talento Humano',
];

const STEPS = [
  { id: 1, label: 'Datos complementarios' },
  { id: 2, label: 'Patrocinador y equipo' },
  { id: 3, label: 'Fases / hitos / entregables' },
  { id: 4, label: 'PETI y comunicaciones' },
  { id: 5, label: 'FURAG' },
  { id: 6, label: 'Gestion documental' },
];

const initialForm = (project) => ({
  dependencia: project?.dependencia || '',
  fechaInicio: project?.fechaInicio || '',
  alcanceDetallado: project?.alcanceDetallado || project?.alcanceDetalle || project?.alcance || '',
  presupuestoEstimado: project?.presupuestoEstimado ?? project?.presupuesto ?? '',
  objetivosEspecificos: Array.isArray(project?.objetivosEspecificos) ? project.objetivosEspecificos : [],
  patrocinador: project?.patrocinador || { nombre: '', cargo: '', entidad: '', procesoSigc: '', procedimientoSigc: '' },
  equipoTrabajo: Array.isArray(project?.equipoTrabajo) ? project.equipoTrabajo : [],
  fases: Array.isArray(project?.fases) ? project.fases : [],
  peti: project?.peti ?? null,
  vigenciaPeti: project?.vigenciaPeti || '',
  estrategiaPeti: project?.estrategiaPeti || null,
  tienePlanComunicaciones: project?.tienePlanComunicaciones ?? null,
  furag: project?.furag || {},
  viabilizacionPdf: null,
  actaConstitucionPdf: null,
  cronogramaPdf: null,
  planComunicacionesPdf: null,
});

const sumPonderacion = (items = []) => items.reduce((sum, item) => sum + (parseFloat(item?.ponderacion) || 0), 0);

const toDateOnly = (value) => {
  if (!value) return null;
  const parsed = new Date(`${value}T00:00:00`);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
};

const isBeforeDate = (value, minValue) => {
  const date = toDateOnly(value);
  const minDate = toDateOnly(minValue);
  return Boolean(date && minDate && date < minDate);
};

const getStorageKey = (project) => {
  const projectId = project?.id || project?.codigo || project?.projectId;
  return projectId ? `wizard_onboarding_${projectId}` : null;
};

const loadSavedState = (project) => {
  const key = getStorageKey(project);
  if (!key) return null;
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return {
      step: typeof parsed.step === 'number' ? parsed.step : 1,
      form: parsed.form || null,
    };
  } catch {
    return null;
  }
};

const saveState = (project, step, form) => {
  const key = getStorageKey(project);
  if (!key) return;
  try {
    const serializable = { ...form };
    delete serializable.viabilizacionPdf;
    delete serializable.actaConstitucionPdf;
    delete serializable.cronogramaPdf;
    delete serializable.planComunicacionesPdf;
    localStorage.setItem(key, JSON.stringify({ step, form: serializable }));
  } catch {
    // silently fail
  }
};

const clearSavedState = (project) => {
  const key = getStorageKey(project);
  if (!key) return;
  try {
    localStorage.removeItem(key);
  } catch {
    // silently fail
  }
};

const ProjectOnboardingWizard = ({
  project,
  saving = false,
  error = '',
  onComplete,
}) => {
  const [savedState] = useState(() => loadSavedState(project));
  const [step, setStep] = useState(savedState?.step || 1);
  const [form, setForm] = useState(() => savedState?.form || initialForm(project));
  const [errors, setErrors] = useState({});
  const [petiCatalog, setPetiCatalog] = useState(null);
  const [petiCatalogLoading, setPetiCatalogLoading] = useState(false);

  useEffect(() => {
    saveState(project, step, form);
  }, [project, step, form]);

  const projectSummary = useMemo(() => ({
    codigo: project?.codigo || project?.id || 'PENDIENTE',
    nombre: project?.nombre || project?.nombreProyecto || 'Proyecto sin nombre',
    objetivo: project?.objetivoGeneral || project?.objetivo || 'Sin objetivo registrado',
    director: project?.directorNombre || project?.director || 'Sin director',
  }), [project]);

  useEffect(() => {
    let active = true;

    const loadPetiCatalog = async () => {
      setPetiCatalogLoading(true);
      try {
        const catalog = await configCatalogService.getPetiCatalog();
        if (active) {
          setPetiCatalog(catalog);
        }
      } catch (catalogError) {
        console.error('Error cargando catalogo PETI:', catalogError);
        if (active) {
          setPetiCatalog(null);
        }
      } finally {
        if (active) {
          setPetiCatalogLoading(false);
        }
      }
    };

    loadPetiCatalog();
    return () => {
      active = false;
    };
  }, []);

  const handleChange = (partialData) => {
    setForm((current) => {
      const merged = { ...current };
      for (const key of Object.keys(partialData)) {
        const value = partialData[key];
        if (value !== null && typeof value === 'object' && !Array.isArray(value) && !(value instanceof File)) {
          merged[key] = { ...(merged[key] || {}), ...value };
        } else {
          merged[key] = value;
        }
      }
      return merged;
    });
    setErrors({});
  };

  const handleObjetivoAdd = () => {
    handleChange({ objetivosEspecificos: [...(form.objetivosEspecificos || []), ''] });
  };

  const handleObjetivoChange = (index, value) => {
    const next = [...(form.objetivosEspecificos || [])];
    next[index] = value;
    handleChange({ objetivosEspecificos: next });
  };

  const handleObjetivoRemove = (index) => {
    handleChange({ objetivosEspecificos: (form.objetivosEspecificos || []).filter((_, i) => i !== index) });
  };

  const validateStep = (targetStep, source = form) => {
    const nextErrors = {};

    if (targetStep === 1) {
      if (!source.dependencia) nextErrors.dependencia = 'Seleccione una dependencia.';
      if (!source.fechaInicio) nextErrors.fechaInicio = 'La fecha de inicio es obligatoria.';
      if (!String(source.alcanceDetallado || '').trim()) nextErrors.alcanceDetallado = 'El alcance detallado es obligatorio.';
      if (source.presupuestoEstimado === '' || source.presupuestoEstimado == null) {
        nextErrors.presupuestoEstimado = 'El presupuesto estimado es obligatorio.';
      } else if (Number(source.presupuestoEstimado) < 0) {
        nextErrors.presupuestoEstimado = 'El presupuesto no puede ser negativo.';
      } else if (Number(source.presupuestoEstimado) > 1e15) {
        nextErrors.presupuestoEstimado = 'El presupuesto no puede superar $9.999.999.999.999.999.';
      }
    }

    if (targetStep === 2) {
      if (!source.patrocinador?.nombre?.trim()) nextErrors.patrocinadorNombre = 'El nombre del patrocinador es obligatorio.';
      if (!source.patrocinador?.cargo?.trim()) nextErrors.patrocinadorCargo = 'El cargo del patrocinador es obligatorio.';
      if (!source.patrocinador?.entidad?.trim()) nextErrors.patrocinadorEntidad = 'La entidad del patrocinador es obligatoria.';
    }

    if (targetStep === 3) {
      if (!(source.fases || []).length) {
        nextErrors.fases = 'Debe configurar al menos una fase con hito y entregable.';
      }

      (source.fases || []).forEach((fase, faseIndex) => {
        if (!fase.nombre?.trim()) nextErrors[`fase_${faseIndex}_nombre`] = 'El nombre de la fase es obligatorio.';
        if (!fase.ponderacion || parseFloat(fase.ponderacion) <= 0) nextErrors[`fase_${faseIndex}_ponderacion`] = 'La ponderacion debe ser mayor a 0.';
        if (!(fase.hitos || []).length) {
          nextErrors[`fase_${faseIndex}_hitos`] = `La fase ${faseIndex + 1} debe tener al menos un hito.`;
        }

        (fase.hitos || []).forEach((hito, hitoIndex) => {
          if (!hito.nombre?.trim()) nextErrors[`hito_${faseIndex}_${hitoIndex}_nombre`] = 'El nombre del hito es obligatorio.';
          if (!hito.ponderacion || parseFloat(hito.ponderacion) <= 0) nextErrors[`hito_${faseIndex}_${hitoIndex}_ponderacion`] = 'La ponderacion del hito debe ser mayor a 0.';
          if (!(hito.entregables || []).length) {
            nextErrors[`hito_${faseIndex}_${hitoIndex}_entregables`] = `El hito ${faseIndex + 1}.${hitoIndex + 1} debe tener al menos un entregable.`;
          }

          (hito.entregables || []).forEach((entregable, entregableIndex) => {
            if (!entregable.nombre?.trim()) nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_nombre`] = 'El nombre del entregable es obligatorio.';
            if (!entregable.ponderacion || parseFloat(entregable.ponderacion) <= 0) nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_ponderacion`] = 'La ponderacion del entregable debe ser mayor a 0.';
            if (!entregable.fechaInicio) nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_fechaInicio`] = 'La fecha de inicio es obligatoria.';
            if (!entregable.fechaLimite) nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_fechaLimite`] = 'La fecha limite es obligatoria.';
            if (entregable.fechaInicio && source.fechaInicio && isBeforeDate(entregable.fechaInicio, source.fechaInicio)) {
              nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_fechaInicio`] = `La fecha de inicio del entregable no puede ser anterior a la fecha de inicio configurada del proyecto (${source.fechaInicio}).`;
            }
            if (entregable.fechaLimite && source.fechaInicio && isBeforeDate(entregable.fechaLimite, source.fechaInicio)) {
              nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_fechaLimite`] = `La fecha limite del entregable no puede ser anterior a la fecha de inicio configurada del proyecto (${source.fechaInicio}).`;
            }
            if (entregable.fechaInicio && entregable.fechaLimite && new Date(entregable.fechaLimite) < new Date(entregable.fechaInicio)) {
              nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_fechaLimite`] = 'La fecha limite debe ser mayor o igual a la fecha de inicio.';
            }
          });

          const sumaEntregables = sumPonderacion(hito.entregables || []);
          if ((hito.entregables || []).length > 0 && Math.abs(sumaEntregables - 100) > 0.01) {
            nextErrors[`hito_${faseIndex}_${hitoIndex}_ponderacion_suma`] = `Los entregables del hito ${faseIndex + 1}.${hitoIndex + 1} deben sumar 100%.`;
          }
        });

        const sumaHitos = sumPonderacion(fase.hitos || []);
        if ((fase.hitos || []).length > 0 && Math.abs(sumaHitos - 100) > 0.01) {
          nextErrors[`fase_${faseIndex}_ponderacion_suma`] = `Los hitos de la fase ${faseIndex + 1} deben sumar 100%.`;
        }
      });

      const sumaFases = sumPonderacion(source.fases || []);
      if ((source.fases || []).length > 0 && Math.abs(sumaFases - 100) > 0.01) {
        nextErrors.fases = 'La suma total de fases debe ser exactamente 100%.';
      }
    }

    if (targetStep === 4) {
      if (source.peti === null) nextErrors.peti = 'Debe indicar si el proyecto esta en PETI.';
      if (source.peti === true) {
        if (!source.vigenciaPeti) nextErrors.vigenciaPeti = 'Seleccione la vigencia PETI.';
        if (!source.estrategiaPeti) nextErrors.estrategiaPeti = 'Seleccione la estrategia PETI.';
      }
      if (source.tienePlanComunicaciones === null) {
        nextErrors.tienePlanComunicaciones = 'Debe indicar si cuenta con plan de comunicaciones.';
      }
    }

    if (targetStep === 5) {
      ['infraestructuraDatos', 'interoperabilidad', 'digitalizacionAutomatizacion', 'contratacionPublica', 'serviciosNube', 'sandbox', 'tecnologiasEmergentes'].forEach((key) => {
        if (!source.furag?.[key]) nextErrors[`furag_${key}`] = 'Debe seleccionar una respuesta.';
      });
    }

    if (targetStep === 6) {
      if (!source.viabilizacionPdf) nextErrors.viabilizacionPdf = 'El documento de viabilidad es obligatorio.';
    }

    return nextErrors;
  };

  const getFirstValidationMessage = (nextErrors) => {
    if (nextErrors.fases) return nextErrors.fases;
    const firstKey = Object.keys(nextErrors)[0];
    return firstKey ? nextErrors[firstKey] : 'Revise los campos marcados.';
  };

  const handleNext = () => {
    const nextErrors = validateStep(step);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    setStep((current) => Math.min(current + 1, STEPS.length));
  };

  const buildPayload = () => ({
    dependencia: form.dependencia,
    fechaInicio: form.fechaInicio,
    alcanceDetallado: String(form.alcanceDetallado || '').trim(),
    presupuestoEstimado: Number(form.presupuestoEstimado) || 0,
    objetivosEspecificos: (form.objetivosEspecificos || []).map((value) => value?.trim()).filter(Boolean),
    patrocinador: {
      nombre: form.patrocinador?.nombre || '',
      cargo: form.patrocinador?.cargo || '',
      entidad: form.patrocinador?.entidad || '',
      procesoSigc: form.patrocinador?.procesoSigc || null,
      procedimientoSigc: form.patrocinador?.procedimientoSigc || null,
    },
    equipoTrabajo: (form.equipoTrabajo || []).map((member) => ({
      nombre: member.nombre,
      cargo: member.cargo,
      rol: member.rol,
    })),
    fases: (form.fases || []).map((fase) => ({
      nombre: fase.nombre,
      descripcion: fase.descripcion || null,
      ponderacion: parseFloat(fase.ponderacion),
      hitos: (fase.hitos || []).map((hito) => ({
        nombre: hito.nombre,
        descripcion: hito.descripcion || null,
        ponderacion: parseFloat(hito.ponderacion),
        entregables: (hito.entregables || []).map((entregable) => ({
          nombre: entregable.nombre,
          ponderacion: parseFloat(entregable.ponderacion),
          fechaInicio: entregable.fechaInicio,
          fechaLimite: entregable.fechaLimite,
        })),
      })),
    })),
    peti: form.peti,
    vigenciaPeti: form.peti === true ? form.vigenciaPeti : null,
    estrategiaPeti: form.peti === true ? form.estrategiaPeti : null,
    tienePlanComunicaciones: form.tienePlanComunicaciones,
    furag: {
      infraestructuraDatos: form.furag?.infraestructuraDatos || null,
      interoperabilidad: form.furag?.interoperabilidad || null,
      digitalizacionAutomatizacion: form.furag?.digitalizacionAutomatizacion || null,
      contratacionPublica: form.furag?.contratacionPublica || null,
      serviciosNube: form.furag?.serviciosNube || null,
      sandbox: form.furag?.sandbox || null,
      tecnologiasEmergentes: form.furag?.tecnologiasEmergentes || null,
    },
  });

  const handleSubmit = (event) => {
    event.preventDefault();

    for (const targetStep of STEPS.map((item) => item.id)) {
      const nextErrors = validateStep(targetStep);
      if (Object.keys(nextErrors).length > 0) {
        setStep(targetStep);
        setErrors(nextErrors);
        return;
      }
    }

    setErrors({});
    clearSavedState(project);
    onComplete?.(buildPayload(), {
      viabilizacionPdf: form.viabilizacionPdf || null,
      actaConstitucionPdf: form.actaConstitucionPdf || null,
      cronogramaPdf: form.cronogramaPdf || null,
      planComunicacionesPdf: form.planComunicacionesPdf || null,
    });
  };

  const renderStep = () => {
    if (step === 1) {
      return (
        <div className="step-form">
          <h3 className="step-title">Datos complementarios</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">Dependencia Responsable *</label>
              <select
                className={`form-input ${errors.dependencia ? 'input-error' : ''}`}
                value={form.dependencia || ''}
                onChange={(event) => handleChange({ dependencia: event.target.value })}
              >
                <option value="">Seleccione una dependencia</option>
                {DEPENDENCIAS.map((dependencia) => (
                  <option key={dependencia} value={dependencia}>{dependencia}</option>
                ))}
              </select>
              {errors.dependencia && <span className="error-text">{errors.dependencia}</span>}
            </div>

            <div className="form-group">
              <label className="form-label">Fecha de Inicio *</label>
              <input
                type="date"
                className={`form-input ${errors.fechaInicio ? 'input-error' : ''}`}
                value={form.fechaInicio || ''}
                onChange={(event) => handleChange({ fechaInicio: event.target.value })}
              />
              {errors.fechaInicio && <span className="error-text">{errors.fechaInicio}</span>}
            </div>

            <div className="form-group">
              <label className="form-label">Presupuesto estimado *</label>
              <input
                type="number"
                min="0"
                max="9999999999999999.99"
                step="0.01"
                className={`form-input ${errors.presupuestoEstimado ? 'input-error' : ''}`}
                value={form.presupuestoEstimado}
                onChange={(event) => handleChange({ presupuestoEstimado: event.target.value })}
                placeholder="0"
              />
              {errors.presupuestoEstimado && <span className="error-text">{errors.presupuestoEstimado}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Alcance detallado *</label>
            <textarea
              className={`form-input form-textarea ${errors.alcanceDetallado ? 'input-error' : ''}`}
              value={form.alcanceDetallado || ''}
              onChange={(event) => handleChange({ alcanceDetallado: event.target.value })}
              rows={5}
              placeholder="Describa alcance, limites y resultados esperados."
            />
            {errors.alcanceDetallado && <span className="error-text">{errors.alcanceDetallado}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Objetivos especificos</label>
            {(form.objetivosEspecificos || []).map((objective, index) => (
              <div key={`objective-${index}`} className="array-field-row">
                <input
                  className="form-input"
                  value={objective}
                  onChange={(event) => handleObjetivoChange(index, event.target.value)}
                  placeholder={`Objetivo especifico ${index + 1}`}
                />
                <button type="button" className="btn-icon-danger" onClick={() => handleObjetivoRemove(index)}>
                  x
                </button>
              </div>
            ))}
            <button type="button" className="btn-add" onClick={handleObjetivoAdd}>
              + Agregar objetivo especifico
            </button>
          </div>
        </div>
      );
    }

    if (step === 2) {
      return <Paso2PatrocinadorEquipo data={form} onChange={handleChange} errors={errors} />;
    }

    if (step === 3) {
      return (
        <Paso3FasesHitosEntregables
          data={form}
          onChange={handleChange}
          errors={errors}
          allowEmpty={false}
        />
      );
    }

    if (step === 4) {
      return (
        <Paso4PetiComunicaciones
          data={form}
          onChange={handleChange}
          errors={errors}
          catalog={petiCatalog}
          loadingCatalog={petiCatalogLoading}
        />
      );
    }

    if (step === 5) {
      return <Paso5Furag data={form} onChange={handleChange} errors={errors} />;
    }

    return <Paso6GestionDocumental data={form} onChange={handleChange} errors={errors} />;
  };

  return (
    <div className="project-onboarding">
      <div className="project-onboarding__shell">
        <form className="project-onboarding__panel card-surface" onSubmit={handleSubmit}>
          <div className="project-onboarding__header project-onboarding__hero">
            <div className="project-onboarding__hero-copy">
              <span className="modal-flow-badge">Momento 2 - Director de Proyecto</span>
              <h2 className="page-title">Completar informacion del proyecto</h2>
              <p className="page-subtitle">
                El proyecto fue registrado por el Gestor. Complete la informacion pendiente para habilitar los modulos operativos.
              </p>
            </div>
            <div className="project-onboarding__status-card">
              <span>Estado actual</span>
              <strong>Pendiente de Completar</strong>
              <small>Los modulos operativos siguen bloqueados hasta guardar este asistente.</small>
            </div>
          </div>

          <div className="project-onboarding__locked info-banner">
            <LockKeyhole size={16} />
            <div>
              <strong>Datos protegidos por el gestor</strong>
              <p>Codigo, nombre, objetivo y Director asignado no pueden modificarse desde este asistente.</p>
            </div>
          </div>

          <section className="project-onboarding__locked-grid" aria-label="Datos base protegidos">
            <article className="project-onboarding__locked-card">
              <span>Codigo</span>
              <strong>{projectSummary.codigo}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Proyecto</span>
              <strong>{projectSummary.nombre}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Director asignado</span>
              <strong>{projectSummary.director}</strong>
            </article>
            <article className="project-onboarding__locked-card project-onboarding__locked-card--wide">
              <span>Objetivo registrado</span>
              <p>{projectSummary.objetivo}</p>
            </article>
          </section>

          <div className="project-onboarding__stepbar-heading">
            <span>Asistente obligatorio de completitud</span>
            <strong>Paso {step} de {STEPS.length}</strong>
          </div>

          <div className="project-onboarding__steps">
            {STEPS.map((item) => (
              <button
                key={item.id}
                type="button"
                className={`project-onboarding__step ${step === item.id ? 'active' : ''}`}
                onClick={() => {
                  if (item.id <= step) setStep(item.id);
                }}
              >
                {String(item.id).padStart(2, '0')} {item.label}
              </button>
            ))}
          </div>

          {renderStep()}

          {Object.keys(errors).length > 0 && (
            <div className="error-banner">
              <AlertTriangle size={18} />
              <span>{getFirstValidationMessage(errors)}</span>
            </div>
          )}

          {error ? <div className="error-banner">{error}</div> : null}

          <div className="project-onboarding__footer">
            <span />
            <div className="compact-toolbar">
              {step > 1 ? (
                <button type="button" className="btn-ghost" onClick={() => setStep((current) => current - 1)}>
                  <ArrowLeft size={16} />
                  Volver
                </button>
              ) : null}

              {step < STEPS.length ? (
                <button type="button" className="btn-primary" onClick={handleNext} disabled={saving}>
                  Siguiente
                  <ArrowRight size={16} />
                </button>
              ) : (
                <button type="submit" className="btn-primary" disabled={saving}>
                  <Save size={16} />
                  {saving ? 'Guardando...' : 'Completar proyecto'}
                </button>
              )}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProjectOnboardingWizard;
