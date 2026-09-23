import { useEffect, useState } from 'react';
import { ArrowLeft, ArrowRight, CheckCircle, FileText, Target, Building2, User, Zap } from 'lucide-react';
import projectService from '../../../services/projectService';
import configCatalogService from '../../../services/configCatalogService';
import { SpellCheckInput } from '../../common/SpellCheckInput/SpellCheckInput';
import SpellCheckerTextarea from '../../common/SpellCheckerTextarea';
import { AutocompleteSelect } from '../../common/AutocompleteSelect';
import '../../projects/ProjectOnboardingWizard.css';
import './EditProjectModal.css';

const MOMENTOS = [
  { id: 1, icon: FileText, label: 'MOMENTO 1', title: 'Datos Generales', desc: 'Información básica: nombre, dependencia, director y fecha de inicio.' },
  { id: 2, icon: Target, label: 'MOMENTO 2', title: 'Objetivos del Proyecto', desc: 'Defina el objetivo general y los objetivos específicos.' },
  { id: 3, icon: Zap, label: 'MOMENTO 3', title: 'PETI (Tecnologías de Información)', desc: 'Configure si el proyecto aplica al Plan Estratégico de TI.' },
];

const normalizeOption = (option) => {
  if (!option) return null;
  if (typeof option === 'string') return { value: option, label: option };
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

const getDirectorId = (d) => d?.id ?? d?.usuarioId ?? d?.userId ?? '';
const getDirectorName = (d) => d?.nombre || d?.name || d?.username || '';
const getDirectorEmail = (d) => d?.correo || d?.email || '';
const getDirectorRole = (d) => d?.rolNombre || d?.rolCodigo || d?.rol || '';

const extractApiMessage = (error) => (
  error?.response?.data?.detail
  || error?.response?.data?.message
  || error?.response?.data?.title
  || error?.message
  || 'No se pudo guardar el proyecto.'
);

const EditProjectModal = ({ projectId, onClose, onSaved }) => {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    nombre: '',
    dependencia: '',
    objetivoGeneral: '',
    objetivosEspecificos: [],
    directorUsuarioId: '',
    director: '',
    correoDirector: '',
    fechaInicio: '',
    peti: false,
    vigenciaPeti: '',
    estrategiaPeti: '',
  });
  const [projectMeta, setProjectMeta] = useState(null);
  const [directorOptions, setDirectorOptions] = useState([]);
  const [directorsLoading, setDirectorsLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [submitError, setSubmitError] = useState('');
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [errors, setErrors] = useState({});

  const [dependencias, setDependencias] = useState([]);
  const [vigencias, setVigencias] = useState([]);
  const [estrategias, setEstrategias] = useState([]);
  const [catalogsLoading, setCatalogsLoading] = useState(true);

  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = ''; };
  }, []);

  useEffect(() => {
    if (!projectId) return;
    let mounted = true;

    const loadData = async () => {
      try {
        const [projectResult, directorsResult] = await Promise.allSettled([
          projectService.getById(projectId),
          projectService.getAssignableDirectors(),
        ]);

        if (!mounted) return;

        if (projectResult.status === 'fulfilled') {
          const p = projectResult.value?.data || projectResult.value || {};
          setProjectMeta({
            id: p.id || p.codigo || '',
            nombre: p.nombre || '',
            dependencia: p.dependencia || '',
            director: p.director || p.directorNombre || '',
            vigencia: p.vigenciaPeti || 'Fiscal 2026',
          });
          setFormData({
            nombre: p.nombre || '',
            dependencia: p.dependencia || '',
            objetivoGeneral: p.objetivoGeneral || '',
            objetivosEspecificos: Array.isArray(p.objetivosEspecificos) ? p.objetivosEspecificos : [],
            directorUsuarioId: p.directorUsuarioId || p.directorId || '',
            director: p.director || p.directorNombre || '',
            correoDirector: p.correoDirector || '',
            fechaInicio: p.fechaInicio || '',
            peti: p.peti || false,
            vigenciaPeti: p.vigenciaPeti || '',
            estrategiaPeti: p.estrategiaPeti || '',
          });
        } else {
          setSubmitError('No se pudo cargar la información del proyecto.');
        }

        if (directorsResult.status === 'fulfilled') {
          setDirectorOptions(Array.isArray(directorsResult.value) ? directorsResult.value : []);
        }
      } catch {
        if (mounted) setSubmitError('No se pudo cargar la información del proyecto.');
      } finally {
        if (mounted) {
          setLoading(false);
          setDirectorsLoading(false);
        }
      }
    };

    loadData();
    return () => { mounted = false; };
  }, [projectId]);

  useEffect(() => {
    let active = true;
    const loadCatalogs = async () => {
      setCatalogsLoading(true);
      try {
        const [depResult, petiResult] = await Promise.allSettled([
          configCatalogService.listarValoresParametrica('DEPENDENCIA'),
          configCatalogService.getPetiCatalog(),
        ]);
        if (!active) return;

        if (depResult.status === 'fulfilled' && Array.isArray(depResult.value)) {
          setDependencias(depResult.value);
        }
        if (petiResult.status === 'fulfilled') {
          const cat = petiResult.value;
          if (Array.isArray(cat?.vigencias)) setVigencias(cat.vigencias);
          if (Array.isArray(cat?.estrategias)) setEstrategias(cat.estrategias);
        }
      } catch {
        // catalogs will remain empty
      } finally {
        if (active) setCatalogsLoading(false);
      }
    };
    loadCatalogs();
    return () => { active = false; };
  }, []);

  const updateField = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const handleDirectorChange = (value) => {
    const director = directorOptions.find((d) => String(getDirectorId(d)) === String(value));
    setFormData((prev) => ({
      ...prev,
      directorUsuarioId: value,
      director: director ? getDirectorName(director) : '',
      correoDirector: director ? getDirectorEmail(director) : '',
    }));
  };

  const handleObjetivoAdd = () => {
    setFormData((prev) => ({
      ...prev,
      objetivosEspecificos: [...(prev.objetivosEspecificos || []), ''],
    }));
  };

  const handleObjetivoChange = (index, value) => {
    const nuevos = [...(formData.objetivosEspecificos || [])];
    nuevos[index] = value;
    setFormData((prev) => ({ ...prev, objetivosEspecificos: nuevos }));
  };

  const handleObjetivoRemove = (index) => {
    setFormData((prev) => ({
      ...prev,
      objetivosEspecificos: (prev.objetivosEspecificos || []).filter((_, i) => i !== index),
    }));
  };

  const validateStep = (currentStep) => {
    const nextErrors = {};
    if (currentStep === 1) {
      if (!formData.nombre.trim()) nextErrors.nombre = 'El nombre es obligatorio.';
    }
    if (currentStep === 2) {
      if (!formData.objetivoGeneral.trim()) nextErrors.objetivoGeneral = 'El objetivo general es obligatorio.';
      const objetivosValidos = (formData.objetivosEspecificos || []).filter((o) => o?.trim());
      if (objetivosValidos.length === 0) {
        nextErrors.objetivosEspecificos = 'Debe agregar al menos un objetivo específico.';
      }
    }
    setErrors(nextErrors);
    return nextErrors;
  };

  const handleNext = () => {
    if (Object.keys(validateStep(step)).length > 0) return;
    setStep((prev) => Math.min(prev + 1, MOMENTOS.length));
  };

  const handleBack = () => {
    setStep((prev) => Math.max(prev - 1, 1));
  };

  const handleSubmit = async () => {
    if (step < MOMENTOS.length) return;
    if (Object.keys(validateStep(step)).length > 0) return;

    setSaving(true);
    setSubmitError('');

    try {
      const payload = {
        nombre: formData.nombre.trim(),
        dependencia: formData.dependencia || null,
        objetivoGeneral: formData.objetivoGeneral.trim(),
        objetivosEspecificos: formData.objetivosEspecificos.filter(Boolean),
        director: formData.director || null,
        correoDirector: formData.correoDirector || null,
        fechaInicio: formData.fechaInicio || null,
        peti: formData.peti,
        vigenciaPeti: formData.vigenciaPeti || null,
        estrategiaPeti: formData.estrategiaPeti || null,
      };
      await projectService.update(projectId, payload);
      setSubmitSuccess(true);
      setTimeout(() => {
        if (onSaved) onSaved();
        onClose();
      }, 1200);
    } catch (err) {
      setSubmitError(extractApiMessage(err));
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="edit-modal-overlay" onClick={onClose}>
        <div className="edit-modal-shell" onClick={(e) => e.stopPropagation()}>
          <div className="edit-modal-panel" style={{ display: 'grid', placeItems: 'center' }}>
            <span style={{ color: 'var(--text-muted)' }}>Cargando proyecto...</span>
          </div>
        </div>
      </div>
    );
  }

  if (submitSuccess) {
    return (
      <div className="edit-modal-overlay" onClick={onClose}>
        <div className="edit-modal-shell" onClick={(e) => e.stopPropagation()}>
          <div className="edit-modal-panel" style={{ display: 'grid', placeItems: 'center' }}>
            <div style={{ textAlign: 'center', display: 'grid', gap: '12px', justifyItems: 'center' }}>
              <CheckCircle size={56} color="#16a34a" />
              <h2 style={{ margin: 0, fontSize: '1.25rem' }}>Proyecto actualizado</h2>
              <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.9rem' }}>Los cambios se guardaron correctamente.</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="edit-modal-overlay" onClick={onClose}>
      <div className="edit-modal-shell" onClick={(e) => e.stopPropagation()}>
        <div className="edit-modal-panel">

          {/* Header */}
          <div className="edit-modal-header">
            <div className="edit-modal-header__left">
              <button className="edit-modal-back" onClick={onClose} type="button">
                <ArrowLeft size={14} />
                <span>Volver</span>
              </button>
              <div className="edit-modal-header__title-group">
                <h1 className="edit-modal-header__title">Editar Proyecto</h1>
                <span className="edit-modal-header__badge">EN EDICIÓN</span>
              </div>
            </div>
            <div className="edit-modal-header__right">
              <span className="edit-modal-autosave">
                <span className="edit-modal-autosave__dot" />
                Autoguardado: <strong>activo</strong>
              </span>
            </div>
          </div>

          <p className="edit-modal-header__subtitle">
            Modifique la información estructurada del proyecto paso a paso.
          </p>

          {/* Summary Card */}
          {projectMeta && (
            <div className="edit-modal-summary">
              <div className="edit-modal-summary__left">
                <span className="edit-modal-summary__code">{projectMeta.id}</span>
                <span className="edit-modal-summary__name">{projectMeta.nombre || 'Sin nombre'}</span>
                <span className="edit-modal-summary__vigencia">Vigencia {projectMeta.vigencia}</span>
              </div>
              <div className="edit-modal-summary__right">
                <div className="edit-modal-summary__field">
                  <Building2 size={14} />
                  <div>
                    <span className="edit-modal-summary__label">ÁREA RESPONSABLE</span>
                    <span className="edit-modal-summary__value">{projectMeta.dependencia || 'Sin asignar'}</span>
                  </div>
                </div>
                <div className="edit-modal-summary__field">
                  <User size={14} />
                  <div>
                    <span className="edit-modal-summary__label">LÍDER / ROL ASIGNADO</span>
                    <span className="edit-modal-summary__value">{projectMeta.director || 'Sin asignar'}</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Moments Navigation */}
          <div className="edit-modal-moments">
            <div className="edit-modal-moments__heading">
              <span>MOMENTOS DE EDICIÓN</span>
              <strong>Paso {step} de {MOMENTOS.length}</strong>
            </div>
            <div className="edit-modal-moments__cards">
              {MOMENTOS.map((m) => {
                const Icon = m.icon;
                const isCompleted = step > m.id;
                const isActive = step === m.id;
                return (
                  <button
                    key={m.id}
                    type="button"
                    className={`edit-modal-moment ${isCompleted ? 'completed' : ''} ${isActive ? 'active' : ''}`}
                    onClick={(e) => { e.stopPropagation(); if (m.id < step) setStep(m.id); }}
                  >
                    <div className="edit-modal-moment__icon">
                      {isCompleted ? <CheckCircle size={16} /> : <Icon size={16} />}
                    </div>
                    <div className="edit-modal-moment__text">
                      <span className="edit-modal-moment__label">{m.label}</span>
                      <span className="edit-modal-moment__title">{m.title}</span>
                    </div>
                    {isActive && <span className="edit-modal-moment__current">• ACTUAL</span>}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Form Body */}
          <form className="edit-modal-form" onSubmit={(e) => e.preventDefault()}>

            {step === 1 && (
              <div className="edit-modal-step">
                <div className="edit-modal-step__header">
                  <span className="edit-modal-step__dot" />
                  <span className="edit-modal-step__title">Momento 1 — Datos Generales</span>
                </div>
                <p className="edit-modal-step__desc">
                  Información básica del proyecto: nombre, dependencia, director y fecha de inicio.
                </p>

                <div className="edit-modal-fields">
                  <div className="edit-modal-field">
                    <label className="edit-modal-field__label">Nombre del Proyecto *</label>
                    <SpellCheckInput
                      className={`edit-modal-field__input ${errors.nombre ? 'input-error' : ''}`}
                      value={formData.nombre}
                      onChange={(e) => updateField('nombre', e.target.value)}
                      placeholder="Nombre del proyecto"
                    />
                    {errors.nombre && <span className="edit-modal-field__error">{errors.nombre}</span>}
                  </div>

                  <div className="edit-modal-field">
                    <label className="edit-modal-field__label">Dependencia</label>
                    <AutocompleteSelect
                      value={formData.dependencia || ''}
                      onChange={(val) => updateField('dependencia', val)}
                      options={dependencias.map((dep) => ({ value: dep, label: dep }))}
                      placeholder={catalogsLoading ? 'Cargando...' : 'Seleccione dependencia'}
                      allLabel="Sin dependencia"
                      allValue=""
                    />
                  </div>

                  <div className="edit-modal-field">
                    <label className="edit-modal-field__label">Fecha de Inicio</label>
                    <input
                      type="date"
                      className="edit-modal-field__input"
                      value={formData.fechaInicio || ''}
                      onChange={(e) => updateField('fechaInicio', e.target.value)}
                    />
                  </div>

                  <div className="edit-modal-field">
                    <label className="edit-modal-field__label">Director TIC</label>
                    <AutocompleteSelect
                      value={formData.directorUsuarioId || ''}
                      onChange={handleDirectorChange}
                      disabled={directorsLoading}
                      options={directorOptions.map((d) => ({
                        value: getDirectorId(d),
                        label: `${getDirectorName(d)}${getDirectorRole(d) ? ` - ${getDirectorRole(d)}` : ''}`,
                      }))}
                      placeholder={directorsLoading ? 'Cargando...' : 'Seleccione director'}
                      allLabel="Sin director"
                      allValue=""
                    />
                  </div>

                  <div className="edit-modal-field">
                    <label className="edit-modal-field__label">Correo del Director</label>
                    <input
                      className="edit-modal-field__input edit-modal-field__input--muted"
                      value={formData.correoDirector || ''}
                      readOnly
                      placeholder="Se completa automáticamente"
                    />
                  </div>
                </div>
              </div>
            )}

            {step === 2 && (
              <div className="edit-modal-step">
                <div className="edit-modal-step__header">
                  <span className="edit-modal-step__dot" />
                  <span className="edit-modal-step__title">Momento 2 — Objetivos del Proyecto</span>
                </div>
                <p className="edit-modal-step__desc">
                  Defina el objetivo general y los objetivos específicos del proyecto.
                </p>

                <div className="edit-modal-fields">
                  <div className="edit-modal-field edit-modal-field--full">
                    <label className="edit-modal-field__label">Objetivo General *</label>
                    <SpellCheckerTextarea
                      className={`edit-modal-field__input edit-modal-field__textarea ${errors.objetivoGeneral ? 'input-error' : ''}`}
                      value={formData.objetivoGeneral}
                      onChange={(e) => updateField('objetivoGeneral', e.target.value)}
                      placeholder="Describa el objetivo general del proyecto..."
                      rows={4}
                    />
                    {errors.objetivoGeneral && <span className="edit-modal-field__error">{errors.objetivoGeneral}</span>}
                  </div>

                  <div className="edit-modal-field edit-modal-field--full">
                    <label className="edit-modal-field__label">Objetivos Específicos</label>
                    {(formData.objetivosEspecificos || []).map((obj, i) => (
                      <div key={i} className="edit-modal-array-row">
                        <SpellCheckInput
                          className="edit-modal-field__input"
                          value={obj}
                          onChange={(e) => handleObjetivoChange(i, e.target.value)}
                          placeholder={`Objetivo específico ${i + 1}`}
                        />
                        <button type="button" className="edit-modal-array-remove" onClick={() => handleObjetivoRemove(i)}>
                          ✕
                        </button>
                      </div>
                    ))}
                    <button type="button" className="edit-modal-array-add" onClick={handleObjetivoAdd}>
                      + Agregar objetivo específico
                    </button>
                    {errors.objetivosEspecificos && <span className="edit-modal-field__error">{errors.objetivosEspecificos}</span>}
                  </div>
                </div>
              </div>
            )}

            {step === 3 && (
              <div className="edit-modal-step">
                <div className="edit-modal-step__header">
                  <span className="edit-modal-step__dot" />
                  <span className="edit-modal-step__title">Momento 3 — Plan Estratégico de TI (PETI)</span>
                  <span className="edit-modal-step__badge">Lineamiento MiniTIC 2024</span>
                </div>
                <p className="edit-modal-step__desc">
                  Configure si la iniciativa articula o impacta la infraestructura, plataformas y normativas de Tecnologías de la Información institucional.
                </p>

                <div className="edit-modal-fields">
                  <div className="edit-modal-field edit-modal-field--full">
                    <label className="edit-modal-field__label">¿Aplica al Plan Estratégico de TI (PETI)?</label>
                    <p className="edit-modal-field__hint">
                      Marque "Sí" si este proyecto requiere asignación técnica, licencias de software o integración con el ecosistema de TI.
                    </p>
                    <div className="edit-modal-toggle-group">
                      <button
                        type="button"
                        className={`edit-modal-toggle ${formData.peti ? 'edit-modal-toggle--active' : ''}`}
                        onClick={() => updateField('peti', true)}
                      >
                        {formData.peti && <CheckCircle size={14} />}
                        Sí
                      </button>
                      <button
                        type="button"
                        className={`edit-modal-toggle ${!formData.peti ? 'edit-modal-toggle--active' : ''}`}
                        onClick={() => updateField('peti', false)}
                      >
                        No
                      </button>
                    </div>
                  </div>

                  {formData.peti && (
                    <>
                      <div className="edit-modal-field">
                        <label className="edit-modal-field__label">Vigencia PETI</label>
                        <AutocompleteSelect
                          value={formData.vigenciaPeti || ''}
                          onChange={(val) => updateField('vigenciaPeti', val)}
                          options={ensureSelectedValue(vigencias.map((v) => ({ value: v, label: v })), formData.vigenciaPeti)}
                          placeholder={catalogsLoading ? 'Cargando vigencias...' : 'Seleccione vigencia'}
                          allLabel="Sin vigencia"
                          allValue=""
                        />
                        <span className="edit-modal-field__hint">Opciones administradas desde Configuración de Seguridad &gt; Listas: VIGENCIA_PETI.</span>
                      </div>

                      <div className="edit-modal-field">
                        <label className="edit-modal-field__label">Estrategia PETI Institucional</label>
                        <AutocompleteSelect
                          value={formData.estrategiaPeti || ''}
                          onChange={(val) => updateField('estrategiaPeti', val)}
                          options={ensureSelectedValue(estrategias.map(normalizeOption).filter(Boolean), formData.estrategiaPeti)}
                          placeholder={catalogsLoading ? 'Cargando estrategias...' : 'Seleccione estrategia'}
                          allLabel="Sin estrategia"
                          allValue=""
                        />
                        <span className="edit-modal-field__hint">Opciones administradas desde Configuración de Seguridad &gt; Listas: ESTRATEGIA_PETI.</span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            )}

            {submitError && (
              <div className="edit-modal-error">
                <span>{submitError}</span>
              </div>
            )}

            {/* Footer */}
            <div className="edit-modal-footer">
              <button
                type="button"
                className="edit-modal-footer__back"
                onClick={step === 1 ? onClose : handleBack}
                disabled={saving}
              >
                <ArrowLeft size={14} />
                {step === 1 ? 'Cancelar' : 'Anterior'}
              </button>

              <div className="edit-modal-footer__actions">
                {step < MOMENTOS.length ? (
                  <button type="button" className="edit-modal-footer__next" onClick={handleNext}>
                    Siguiente
                    <ArrowRight size={14} />
                  </button>
                ) : (
                  <button type="button" className="edit-modal-footer__submit" disabled={saving} onClick={handleSubmit}>
                    {saving ? 'Guardando...' : 'Guardar Cambios'}
                  </button>
                )}
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default EditProjectModal;
