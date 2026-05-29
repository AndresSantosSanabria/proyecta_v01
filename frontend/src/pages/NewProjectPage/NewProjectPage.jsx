import { useEffect, useRef, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Save, CheckCircle, AlertCircle, AlertTriangle, X } from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import { usePermission } from '../../hooks/usePermission';
import Stepper from '../../components/features/wizard/Stepper';
import Paso1DatosGenerales from '../../components/features/wizard/steps/Paso1DatosGenerales';
import Paso2PatrocinadorEquipo from '../../components/features/wizard/steps/Paso2PatrocinadorEquipo';
import Paso3FasesHitosEntregables from '../../components/features/wizard/steps/Paso3FasesHitosEntregables';
import Paso4PetiComunicaciones from '../../components/features/wizard/steps/Paso4PetiComunicaciones';
import Paso5Furag from '../../components/features/wizard/steps/Paso5Furag';
import Paso6GestionDocumental from '../../components/features/wizard/steps/Paso6GestionDocumental';
import AccessDeniedPage from '../AccessDeniedPage/AccessDeniedPage';
import './NewProjectPage.css';

const DRAFT_STORAGE_KEY = 'proyecta:new-project-draft';

const INITIAL_STATE = {
  nombre: '',
  dependencia: '',
  fechaInicio: '',
  director: '',
  correoDirector: '',
  objetivoGeneral: '',
  objetivosEspecificos: [],
  patrocinador: { nombre: '', cargo: '', entidad: '', procesoSigc: '', procedimientoSigc: '' },
  equipoTrabajo: [],
  fases: [{ nombre: '', descripcion: '', ponderacion: '', hitos: [] }],
  peti: null,
  vigenciaPeti: '',
  estrategiaPeti: null,
  tienePlanComunicaciones: null,
  furag: {},
  viabilizacionPdf: null,
  actaConstitucionPdf: null,
  cronogramaPdf: null,
  planComunicacionesPdf: null,
};

const STEP_COMPONENTS = [
  Paso1DatosGenerales,
  Paso2PatrocinadorEquipo,
  Paso3FasesHitosEntregables,
  Paso4PetiComunicaciones,
  Paso5Furag,
  Paso6GestionDocumental,
];

const readSavedDraft = () => {
  if (typeof window === 'undefined') return null;

  try {
    const savedDraft = window.localStorage.getItem(DRAFT_STORAGE_KEY);
    if (!savedDraft) return null;

    const parsed = JSON.parse(savedDraft);
    if (!parsed) return null;

    return {
      formData: parsed.formData || null,
      currentStep: Number(parsed.currentStep) || 1,
    };
  } catch (error) {
    console.warn('No se pudo restaurar el borrador del proyecto:', error);
    return null;
  }
};

const NewProjectPage = () => {
  const navigate = useNavigate();
  const savedDraft = readSavedDraft();
  const [currentStep, setCurrentStep] = useState(savedDraft?.currentStep || 1);
  const [formData, setFormData] = useState(() => (
    savedDraft?.formData ? { ...INITIAL_STATE, ...savedDraft.formData } : INITIAL_STATE
  ));
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [uploadState, setUploadState] = useState({}); // { tipo: 'pending'|'uploading'|'done'|'error' }
  const [snackbar, setSnackbar] = useState(null);
  const [discardModalOpen, setDiscardModalOpen] = useState(false);
  const snackbarTimerRef = useRef(null);
  const wizardContentRef = useRef(null);
  const canCreateProject = usePermission('PROYECTO:CREAR');

  const showSnackbar = useCallback((type, message) => {
    if (snackbarTimerRef.current) {
      window.clearTimeout(snackbarTimerRef.current);
    }

    setSnackbar({ type, message });
    snackbarTimerRef.current = window.setTimeout(() => {
      setSnackbar(null);
      snackbarTimerRef.current = null;
    }, 4200);
  }, []);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        navigate(-1);
      }
    };

    window.addEventListener('keydown', handleEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleEscape);
      if (snackbarTimerRef.current) {
        window.clearTimeout(snackbarTimerRef.current);
      }
    };
  }, [navigate]);

  useEffect(() => {
    if (!canCreateProject || submitSuccess) return;

    const persistDraft = window.setTimeout(() => {
      try {
        const sanitizedFormData = { ...formData };
        [
          'viabilizacionPdf',
          'actaConstitucionPdf',
          'cronogramaPdf',
          'planComunicacionesPdf',
        ].forEach((field) => {
          sanitizedFormData[field] = null;
        });

        window.localStorage.setItem(
          DRAFT_STORAGE_KEY,
          JSON.stringify({
            currentStep,
            formData: sanitizedFormData,
            updatedAt: new Date().toISOString(),
          })
        );
      } catch (error) {
        console.warn('No se pudo guardar el borrador del proyecto:', error);
      }
    }, 300);

    return () => window.clearTimeout(persistDraft);
  }, [formData, currentStep, canCreateProject, submitSuccess]);

  useEffect(() => {
    if (wizardContentRef.current) {
      wizardContentRef.current.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }, [currentStep]);

  const DOCUMENT_MAP = [
    { field: 'viabilizacionPdf', tipo: 'VIABILIZACION', label: 'ViabilizaciÃ³n' },
    { field: 'actaConstitucionPdf', tipo: 'ACTA_CONSTITUCION', label: 'Acta de ConstituciÃ³n' },
    { field: 'cronogramaPdf', tipo: 'CRONOGRAMA', label: 'Cronograma' },
    { field: 'planComunicacionesPdf', tipo: 'PLAN_COMUNICACIONES', label: 'Plan de Comunicaciones' },
  ];

  const handleStepChange = useCallback((partialData) => {
    setFormData((prev) => {
      const merged = { ...prev };
      for (const key of Object.keys(partialData)) {
        const val = partialData[key];
        if (val !== null && typeof val === 'object' && !Array.isArray(val) && !(val instanceof File)) {
          merged[key] = { ...(merged[key] || {}), ...val };
        } else {
          merged[key] = val;
        }
      }
      return merged;
    });
    setErrors({});
  }, []);

  const clearProjectDraft = useCallback(() => {
    try {
      window.localStorage.removeItem(DRAFT_STORAGE_KEY);
    } catch (error) {
      console.warn('No se pudo limpiar el borrador del proyecto:', error);
    }
  }, []);

  if (!canCreateProject) {
    return <AccessDeniedPage />;
  }

  const validateStep = (step) => {
    const errs = {};
    const d = formData;

    if (step === 1) {
      if (!d.nombre?.trim()) errs.nombre = 'El nombre del proyecto es obligatorio';
      if (!d.dependencia) errs.dependencia = 'Seleccione una dependencia';
      if (!d.fechaInicio) errs.fechaInicio = 'La fecha de inicio es obligatoria';
      if (!d.director?.trim()) errs.director = 'El nombre del director TIC es obligatorio';
      if (!d.correoDirector?.trim()) errs.correoDirector = 'El correo del director es obligatorio';
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.correoDirector)) errs.correoDirector = 'Ingrese un correo v?lido';
      if (!d.objetivoGeneral?.trim()) errs.objetivoGeneral = 'El objetivo general es obligatorio';
    }

    if (step === 2) {
      if (!d.patrocinador?.nombre?.trim()) errs.patrocinadorNombre = 'El nombre del patrocinador es obligatorio';
      if (!d.patrocinador?.cargo?.trim()) errs.patrocinadorCargo = 'El cargo del patrocinador es obligatorio';
      if (!d.patrocinador?.entidad?.trim()) errs.patrocinadorEntidad = 'La entidad del patrocinador es obligatoria';
    }

    if (step === 3) {
      if (!d.fases?.length) errs.fases = 'Debe agregar al menos una fase';
      (d.fases || []).forEach((fase, fi) => {
        if (!fase.nombre?.trim()) errs['fase_' + fi + '_nombre'] = 'El nombre de la fase es obligatorio';
        if (!fase.ponderacion || parseFloat(fase.ponderacion) <= 0) errs['fase_' + fi + '_ponderacion'] = 'La ponderaci?n debe ser mayor a 0';
        if (Math.abs((parseFloat(fase.ponderacion) || 0) - 100) > 0.01) {
          errs['fase_' + fi + '_ponderacion_suma'] = 'La Fase ' + (fi + 1) + ' debe sumar 100%.';
        }
        (fase.hitos || []).forEach((hito, hi) => {
          if (!hito.nombre?.trim()) errs['hito_' + fi + '_' + hi + '_nombre'] = 'El nombre del hito es obligatorio';
          (hito.entregables || []).forEach((ent, ei) => {
            if (!ent.nombre?.trim()) errs['ent_' + fi + '_' + hi + '_' + ei + '_nombre'] = 'El nombre del entregable es obligatorio';
            if (!ent.fechaLimite) errs['ent_' + fi + '_' + hi + '_' + ei + '_fechaLimite'] = 'La fecha l?mite es obligatoria';
          });
          if ((hito.entregables || []).length > 0) {
            const sumaEntregables = (hito.entregables || []).reduce((sum, item) => sum + (parseFloat(item.ponderacion) || 0), 0);
            if (Math.abs(sumaEntregables - 100) > 0.01) {
              errs['hito_' + fi + '_' + hi + '_ponderacion_suma'] = 'El Hito ' + (fi + 1) + '.' + (hi + 1) + ' debe sumar 100%.';
            }
          }
        });
      });
      if ((d.fases || []).length > 0) {
        const sumaFases = (d.fases || []).reduce((sum, item) => sum + (parseFloat(item.ponderacion) || 0), 0);
        if (Math.abs(sumaFases - 100) > 0.01) {
          errs.fases = 'La suma total de fases debe ser exactamente 100%.';
        }
      }
    }

    if (step === 4) {
      if (d.peti === null) errs.peti = 'Debe seleccionar si el proyecto est? en el PETI';
      if (d.peti === true) {
        if (!d.vigenciaPeti) errs.vigenciaPeti = 'Seleccione la vigencia PETI';
        if (!d.estrategiaPeti) errs.estrategiaPeti = 'Seleccione la estrategia PETI';
      }
      if (d.tienePlanComunicaciones === null) errs.tienePlanComunicaciones = 'Debe seleccionar si cuenta con plan de comunicaciones';
    }

    if (step === 5) {
      ['infraestructuraDatos', 'interoperabilidad', 'digitalizacionAutomatizacion', 'contratacionPublica', 'serviciosNube', 'sandbox', 'tecnologiasEmergentes'].forEach((k) => {
        if (!d.furag?.[k]) errs['furag_' + k] = 'Debe seleccionar una respuesta';
      });
    }

    if (step === 6) {
      if (!d.viabilizacionPdf) errs.viabilizacionPdf = 'El documento de viabilizaci?n es obligatorio';
      if (d.tienePlanComunicaciones === true && !d.planComunicacionesPdf) errs.planComunicacionesPdf = 'Debe cargar el Plan de Comunicaciones';
    }

    setErrors(errs);
    return errs;
  };

  const getFirstValidationMessage = (errs, step) => {
    if (step === 3) {
      if (errs.fases) return errs.fases;
      const phaseKey = Object.keys(errs).find((key) => key.startsWith('fase_'));
      if (phaseKey) return errs[phaseKey];
      const hitoKey = Object.keys(errs).find((key) => key.startsWith('hito_'));
      if (hitoKey) return errs[hitoKey];
      const entKey = Object.keys(errs).find((key) => key.startsWith('ent_'));
      if (entKey) return errs[entKey];
    }

    const orderedKeys = ['nombre', 'dependencia', 'fechaInicio', 'director', 'correoDirector', 'objetivoGeneral', 'patrocinadorNombre', 'patrocinadorCargo', 'patrocinadorEntidad', 'vigenciaPeti', 'estrategiaPeti', 'tienePlanComunicaciones', 'viabilizacionPdf', 'planComunicacionesPdf'];
    for (const key of orderedKeys) {
      if (errs[key]) return errs[key];
    }

    const anyKey = Object.keys(errs)[0];
    return anyKey ? errs[anyKey] : 'Revisa los campos marcados.';
  };

  const normalizeApiErrorMessage = (err) => {
    const detail =
      err?.response?.data?.detail ||
      err?.response?.data?.message ||
      err?.response?.data?.title ||
      err?.message ||
      'Error al crear el proyecto';

    return String(detail)
      .replace(/\s+/g, ' ')
      .trim();
  };

  const getApiErrorRecovery = (message) => {
    const normalized = message.toLowerCase();

    if (normalized.includes('entregable') && normalized.includes('fecha límite')) {
      return {
        step: 3,
        message:
          'No puedes crear el proyecto: revisa la fecha límite del entregable. Debe ser mayor o igual a la fecha de inicio del proyecto.',
      };
    }

    if (normalized.includes('documento') && normalized.includes('obligatorio')) {
      return {
        step: 6,
        message: 'No puedes crear el proyecto: faltan documentos obligatorios en Gestión Documental.',
      };
    }

    if (normalized.includes('fecha inicio') || normalized.includes('fecha de inicio')) {
      return {
        step: 1,
        message: 'No puedes crear el proyecto: revisa la fecha de inicio, porque afecta la validez de fases y entregables.',
      };
    }

    return {
      step: null,
      message: `No se pudo crear el proyecto: ${message}`,
    };
  };

  const handleNext = () => {
    const validationErrors = validateStep(currentStep);
    if (Object.keys(validationErrors).length === 0) {
      setCurrentStep((prev) => Math.min(prev + 1, 6));
      showSnackbar('success', 'Paso validado correctamente. Puedes continuar.');
      return;
    }

    showSnackbar('error', 'No puedes continuar: ' + getFirstValidationMessage(validationErrors, currentStep));
  };

  const handleSubmit = async () => {
    const validationErrors = validateStep(currentStep);
    if (Object.keys(validationErrors).length > 0) {
      showSnackbar('error', 'No puedes crear el proyecto: ' + getFirstValidationMessage(validationErrors, currentStep));
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    try {
      const payload = buildPayload(formData);
      const result = await projectService.create(payload);
      const codigo = result.data?.codigo || result.codigo || result.id;
      const projectId = result.data?.id || result.id || codigo;

      const docsToUpload = DOCUMENT_MAP.filter((d) => formData[d.field] instanceof File);
      if (docsToUpload.length === 0) {
        setSubmitSuccess(true);
        clearProjectDraft();
        showSnackbar('success', 'Proyecto creado exitosamente.');
        setTimeout(() => navigate('/proyectos/' + codigo.toLowerCase() + '/avance'), 1500);
        return;
      }

      const newUploadState = {};
      docsToUpload.forEach((d) => { newUploadState[d.tipo] = 'pending'; });
      setUploadState(newUploadState);

      for (const doc of docsToUpload) {
        setUploadState((prev) => ({ ...prev, [doc.tipo]: 'uploading' }));
        try {
          await documentService.cargarDocumento(projectId, doc.tipo, formData[doc.field]);
          setUploadState((prev) => ({ ...prev, [doc.tipo]: 'done' }));
        } catch {
          setUploadState((prev) => ({ ...prev, [doc.tipo]: 'error' }));
        }
      }

      setSubmitSuccess(true);
      clearProjectDraft();
      showSnackbar('success', 'Proyecto creado exitosamente. Se est?n procesando los documentos.');
      setTimeout(() => navigate('/proyectos/' + codigo.toLowerCase() + '/avance'), 1500);
    } catch (err) {
      const apiMessage = normalizeApiErrorMessage(err);
      const recovery = getApiErrorRecovery(apiMessage);

      setSubmitError(recovery.message);
      showSnackbar('error', recovery.message);

      if (recovery.step && recovery.step !== currentStep) {
        setCurrentStep(recovery.step);
      }
    } finally {
      setSubmitting(false);
    }
  };
  const handleBack = () => {
    setCurrentStep((prev) => Math.max(prev - 1, 1));
    setErrors({});
  };

  const handleOpenDiscardModal = () => {
    setDiscardModalOpen(true);
  };

  const handleCancelDiscard = () => {
    setDiscardModalOpen(false);
  };

  const handleConfirmDiscard = () => {
    clearProjectDraft();
    setDiscardModalOpen(false);
    setFormData(INITIAL_STATE);
    setErrors({});
    setUploadState({});
    setCurrentStep(1);
    setSubmitError(null);
    setSnackbar(null);
    navigate(-1);
  };

  const isUploading = Object.values(uploadState).some((s) => s === 'uploading');
  const uploadDone = Object.values(uploadState).length > 0 && Object.values(uploadState).every((s) => s === 'done');

  if (submitSuccess || uploadDone) {
    return (
      <div className="new-project-page">
        <div className="success-container">
          <CheckCircle size={64} color="#16a34a" />
          <h2>Proyecto creado exitosamente</h2>
          {isUploading && <p>Subiendo documentos...</p>}
          {uploadDone && <p>Todos los documentos fueron cargados correctamente.</p>}
          {!isUploading && !uploadDone && <p>Redirigiendo al avance del proyecto...</p>}
          {Object.keys(uploadState).length > 0 && (
            <div className="upload-status-list">
              {DOCUMENT_MAP.filter((d) => uploadState[d.tipo]).map((d) => (
                <div key={d.tipo} className={`upload-status-item ${uploadState[d.tipo]}`}>
                  <span className="upload-status-label">{d.label}</span>
                  <span className="upload-status-icon">
                    {uploadState[d.tipo] === 'uploading' && 'â³'}
                    {uploadState[d.tipo] === 'done' && 'â'}
                    {uploadState[d.tipo] === 'error' && 'â'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  }

  const StepComponent = STEP_COMPONENTS[currentStep - 1];

  return (
    <div
      className="new-project-overlay"
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          navigate(-1);
        }
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
              <h1 id="new-project-title">Nuevo Proyecto TIC</h1>
              <p>Complete la informaciÃ³n bÃ¡sica del proyecto y avance por etapas.</p>
            </div>
          </div>

          <button className="modal-close-btn" onClick={() => navigate(-1)} type="button" aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>

        <div className="new-project-modal-body">
          {snackbar && (
            <div className={`wizard-snackbar ${snackbar.type}`} role="status" aria-live="polite">
              <div className="wizard-snackbar-icon">
                {snackbar.type === 'error' ? <AlertTriangle size={16} /> : <CheckCircle size={16} />}
              </div>
              <span>{snackbar.message}</span>
              <button type="button" className="wizard-snackbar-close" onClick={() => setSnackbar(null)} aria-label="Cerrar notificación">
                <X size={14} />
              </button>
            </div>
          )}

          <div className="wizard-stepper-card">
            <Stepper currentStep={currentStep} />
          </div>

          <div className="wizard-content wizard-content-modal" ref={wizardContentRef}>
            <StepComponent
              data={formData}
              onChange={handleStepChange}
              errors={errors}
            />

            {submitError && (
              <div className="error-banner">
                <AlertCircle size={18} />
                <span>{submitError}</span>
              </div>
            )}

            <div className="wizard-actions">
              {currentStep > 1 && (
                <button className="btn-secondary" onClick={handleBack} disabled={submitting}>
                  <ArrowLeft size={18} />
                  Anterior
                </button>
              )}
              <div className="wizard-actions-right">
                <button type="button" className="btn-ghost-danger btn-discard" onClick={handleOpenDiscardModal} disabled={submitting}>
                  <X size={16} />
                  Eliminar
                </button>
                {currentStep < 6 ? (
                  <button className="btn-primary" onClick={handleNext}>
                    Siguiente
                    <ArrowRight size={18} />
                  </button>
                ) : (
                  <button className="btn-primary btn-submit" onClick={handleSubmit} disabled={submitting}>
                    <Save size={18} />
                    {submitting
                      ? Object.values(uploadState).some((s) => s === 'uploading')
                        ? 'Subiendo documentos...'
                        : 'Creando proyecto...'
                      : 'Crear Proyecto'}
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {discardModalOpen && (
        <div
          className="new-project-discard-overlay"
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              handleCancelDiscard();
            }
          }}
        >
          <div
            className="new-project-discard-modal"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="discard-project-title"
            onClick={(event) => event.stopPropagation()}
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="discard-modal-icon">
              <AlertTriangle size={22} />
            </div>
            <h2 id="discard-project-title">¿Está seguro?</h2>
            <p>
              Esto eliminará el avance de la información que ha venido introduciendo sobre el proyecto.
              El borrador almacenado en el navegador se limpiará y se perderán los cambios no guardados.
            </p>
            <div className="discard-modal-actions">
              <button type="button" className="btn-secondary" onClick={handleCancelDiscard}>
                Cancelar
              </button>
              <button type="button" className="btn-ghost-danger" onClick={handleConfirmDiscard}>
                Eliminar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

function buildPayload(d) {
  return {
    nombre: d.nombre,
    dependencia: d.dependencia,
    director: d.director,
    correoDirector: d.correoDirector,
    objetivoGeneral: d.objetivoGeneral,
    objetivosEspecificos: (d.objetivosEspecificos || []).filter((o) => o?.trim()),
    fechaInicio: d.fechaInicio,
    patrocinador: {
      nombre: d.patrocinador.nombre,
      cargo: d.patrocinador.cargo,
      entidad: d.patrocinador.entidad,
      procesoSigc: d.patrocinador.procesoSigc || null,
      procedimientoSigc: d.patrocinador.procedimientoSigc || null,
    },
    equipoTrabajo: (d.equipoTrabajo || []).map((m) => ({
      nombre: m.nombre,
      cargo: m.cargo,
      rol: m.rol,
    })),
    fases: (d.fases || []).map((fase) => ({
      nombre: fase.nombre,
      descripcion: fase.descripcion || null,
      ponderacion: parseFloat(fase.ponderacion),
      hitos: (fase.hitos || []).map((hito) => ({
        nombre: hito.nombre,
        descripcion: hito.descripcion || null,
        ponderacion: parseFloat(hito.ponderacion),
        entregables: (hito.entregables || []).map((ent) => ({
          nombre: ent.nombre,
          ponderacion: parseFloat(ent.ponderacion),
          fechaLimite: ent.fechaLimite,
        })),
      })),
    })),
    peti: d.peti,
    vigenciaPeti: d.peti === true ? d.vigenciaPeti : null,
    estrategiaPeti: d.peti === true ? d.estrategiaPeti : null,
    tienePlanComunicaciones: d.tienePlanComunicaciones,
    furag: {
      infraestructuraDatos: d.furag?.infraestructuraDatos || null,
      interoperabilidad: d.furag?.interoperabilidad || null,
      digitalizacionAutomatizacion: d.furag?.digitalizacionAutomatizacion || null,
      contratacionPublica: d.furag?.contratacionPublica || null,
      serviciosNube: d.furag?.serviciosNube || null,
      sandbox: d.furag?.sandbox || null,
      tecnologiasEmergentes: d.furag?.tecnologiasEmergentes || null,
    },
  };
}

export default NewProjectPage;
