import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft, ArrowRight, Save, CheckCircle, AlertCircle, Upload } from 'lucide-react';
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

const NewProjectPage = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState(INITIAL_STATE);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [uploadState, setUploadState] = useState({}); // { tipo: 'pending'|'uploading'|'done'|'error' }
  const canCreateProject = usePermission('PROYECTO:CREAR');

  if (!canCreateProject) {
    return <AccessDeniedPage />;
  }

  const DOCUMENT_MAP = [
    { field: 'viabilizacionPdf', tipo: 'VIABILIZACION', label: 'Viabilización' },
    { field: 'actaConstitucionPdf', tipo: 'ACTA_CONSTITUCION', label: 'Acta de Constitución' },
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

  const validateStep = (step) => {
    const errs = {};
    const d = formData;

    if (step === 1) {
      if (!d.nombre?.trim()) errs.nombre = 'El nombre del proyecto es obligatorio';
      if (!d.dependencia) errs.dependencia = 'Seleccione una dependencia';
      if (!d.fechaInicio) errs.fechaInicio = 'La fecha de inicio es obligatoria';
      if (!d.director?.trim()) errs.director = 'El nombre del director TIC es obligatorio';
      if (!d.correoDirector?.trim()) errs.correoDirector = 'El correo del director es obligatorio';
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(d.correoDirector)) errs.correoDirector = 'Ingrese un correo válido';
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
        if (!fase.nombre?.trim()) errs[`fase_${fi}_nombre`] = 'El nombre de la fase es obligatorio';
        if (!fase.ponderacion || parseFloat(fase.ponderacion) <= 0) errs[`fase_${fi}_ponderacion`] = 'La ponderación debe ser mayor a 0';
        (fase.hitos || []).forEach((hito, hi) => {
          if (!hito.nombre?.trim()) errs[`hito_${fi}_${hi}_nombre`] = 'El nombre del hito es obligatorio';
          (hito.entregables || []).forEach((ent, ei) => {
            if (!ent.nombre?.trim()) errs[`ent_${fi}_${hi}_${ei}_nombre`] = 'El nombre del entregable es obligatorio';
            if (!ent.fechaLimite) errs[`ent_${fi}_${hi}_${ei}_fechaLimite`] = 'La fecha límite es obligatoria';
          });
        });
      });
    }

    if (step === 4) {
      if (d.peti === null) errs.peti = 'Debe seleccionar si el proyecto está en el PETI';
      if (d.peti === true) {
        if (!d.vigenciaPeti) errs.vigenciaPeti = 'Seleccione la vigencia PETI';
        if (!d.estrategiaPeti) errs.estrategiaPeti = 'Seleccione la estrategia PETI';
      }
      if (d.tienePlanComunicaciones === null) errs.tienePlanComunicaciones = 'Debe seleccionar si cuenta con plan de comunicaciones';
    }

    if (step === 5) {
      ['infraestructuraDatos', 'interoperabilidad', 'digitalizacionAutomatizacion', 'contratacionPublica', 'serviciosNube', 'sandbox', 'tecnologiasEmergentes'].forEach((k) => {
        if (!d.furag?.[k]) errs[`furag_${k}`] = 'Debe seleccionar una respuesta';
      });
    }

    if (step === 6) {
      if (!d.viabilizacionPdf) errs.viabilizacionPdf = 'El documento de viabilización es obligatorio';
      if (d.tienePlanComunicaciones === true && !d.planComunicacionesPdf) errs.planComunicacionesPdf = 'Debe cargar el Plan de Comunicaciones';
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleNext = () => {
    if (validateStep(currentStep)) {
      setCurrentStep((prev) => Math.min(prev + 1, 6));
    }
  };

  const handleBack = () => {
    setCurrentStep((prev) => Math.max(prev - 1, 1));
    setErrors({});
  };

  const handleSubmit = async () => {
    if (!validateStep(currentStep)) return;

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
        setTimeout(() => navigate(`/proyectos/${codigo.toLowerCase()}/avance`), 1500);
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
      setTimeout(() => navigate(`/proyectos/${codigo.toLowerCase()}/avance`), 1500);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Error al crear el proyecto';
      setSubmitError(msg);
    } finally {
      setSubmitting(false);
    }
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
                    {uploadState[d.tipo] === 'uploading' && '⏳'}
                    {uploadState[d.tipo] === 'done' && '✓'}
                    {uploadState[d.tipo] === 'error' && '✕'}
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
    <div className="new-project-page">
      <div className="page-header">
        <button className="btn-back" onClick={() => navigate(-1)}>
          <ArrowLeft size={20} />
          <span>Volver</span>
        </button>
        <h1>Nuevo Proyecto TIC</h1>
      </div>

      <Stepper currentStep={currentStep} />

      <div className="wizard-content">
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
