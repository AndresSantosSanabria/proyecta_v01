import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AlertTriangle, ArrowLeft, ArrowRight, Check, LockKeyhole, Save, X } from 'lucide-react';
import { usePermission } from '../../hooks/usePermission';
import Paso2PatrocinadorEquipo from '../features/wizard/steps/Paso2PatrocinadorEquipo';
import Paso3FasesHitosEntregables from '../features/wizard/steps/Paso3FasesHitosEntregables';
import { SpellCheckInput } from '../common/SpellCheckInput/SpellCheckInput';
import { AutocompleteSelect } from '../common/AutocompleteSelect';
import Paso4PetiComunicaciones from '../features/wizard/steps/Paso4PetiComunicaciones';
import Paso5Furag from '../features/wizard/steps/Paso5Furag';
import Paso6MatrizRiesgos from '../features/wizard/steps/Paso6MatrizRiesgos';
import Paso7GestionDocumental from '../features/wizard/steps/Paso6GestionDocumental';
import configCatalogService from '../../services/configCatalogService';
import projectService from '../../services/projectService';
import { emitToast } from '../../utils/feedback';
import '../../pages/NewProjectPage/NewProjectPage.css';
import './ProjectOnboardingWizard.css';

const STEPS = [
  { id: 1, label: 'Datos complementarios' },
  { id: 2, label: 'Patrocinador y equipo' },
  { id: 3, label: 'Fases / hitos / entregables' },
  { id: 4, label: 'PETI y comunicaciones' },
  { id: 5, label: 'FURAG' },
  { id: 6, label: 'Matriz de riesgos' },
  { id: 7, label: 'Gestion documental' },
];

const initialForm = (project) => ({
  dependencia: project?.dependencia || '',
  fechaInicio: project?.fechaInicio || new Date().toISOString().slice(0, 10),
  alcanceDetallado: project?.alcanceDetallado || project?.alcanceDetalle || project?.alcance || '',
  presupuestoEstimado: project?.presupuestoEstimado ?? project?.presupuesto ?? '',
  objetivosEspecificos: Array.isArray(project?.objetivosEspecificos) ? project.objetivosEspecificos : [],
  patrocinador: project?.patrocinador || { nombre: '', cargo: '', procesoSigc: '', procedimientoSigc: '' },
  equipoTrabajo: Array.isArray(project?.equipoTrabajo) ? project.equipoTrabajo : [],
  stakeholders: Array.isArray(project?.stakeholders) ? project.stakeholders : [],
  fases: Array.isArray(project?.fases) ? project.fases : [],
  peti: project?.peti ?? null,
  vigenciaPeti: project?.vigenciaPeti || '',
  estrategiaPeti: project?.estrategiaPeti || null,
  // furag stores { [fieldKey]: 'SI'|'NO'|'NO_APLICA' } keyed by the catalog question key
  furag: project?.furag || {},
  riesgosIniciales: Array.isArray(project?.riesgosIniciales) ? project.riesgosIniciales : [],
  viabilizacionPdf: null,
  actaConstitucionPdf: null,
  cronogramaPdf: null,
  planComunicacionesPdf: null,
});

const sumPonderacion = (items = []) => items.reduce((sum, item) => sum + (parseFloat(item?.ponderacion) || 0), 0);

// FURAG field keys are fully dynamic — they come from petiCatalog.furagPreguntas at runtime.
// Do NOT hardcode them here. This allows admins to add/remove FURAG questions from
// the configuration panel without any frontend code changes.

const normalizeFuragValue = (value) => {
  if (value == null) return null;
  const normalized = String(value).trim().toUpperCase().replace(/[\s-]+/g, '_');
  if (normalized === 'NA' || normalized === 'N/A' || normalized === 'NOAPLICA') return 'NO_APLICA';
  if (normalized === 'SI' || normalized === 'S') return 'SI';
  if (normalized === 'NO' || normalized === 'N') return 'NO';
  if (normalized === 'NO_APLICA') return 'NO_APLICA';
  return null;
};

const getFuragValue = (furagState, key) => {
  if (!furagState || !key) return null;

  const exactValue = furagState[key];
  if (exactValue != null) return exactValue;

  const normalizedKey = String(key).toLowerCase().replace(/[^a-z0-9]/g, '');
  const entry = Object.entries(furagState).find(([candidateKey]) => {
    const normalizedCandidate = String(candidateKey).toLowerCase().replace(/[^a-z0-9]/g, '');
    return normalizedCandidate === normalizedKey
      || normalizedCandidate.includes(normalizedKey)
      || normalizedKey.includes(normalizedCandidate);
  });

  return entry ? entry[1] : null;
};

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

const buildDraftPayload = (step, form, fasesCompletadas) => ({
  faseActual: step,
  fasesCompletadas: fasesCompletadas || {},
  datosFase1: {
    dependencia: form.dependencia || '',
    fechaInicio: form.fechaInicio || '',
    presupuestoEstimado: form.presupuestoEstimado || '',
    alcance: form.alcanceDetallado || '',
    objetivosEspecificos: form.objetivosEspecificos || [],
  },
  datosFase2: {
    patrocinador: form.patrocinador || null,
    equipoTrabajo: form.equipoTrabajo || [],
    stakeholders: form.stakeholders || [],
  },
  datosFase3: {
    fases: form.fases || [],
  },
    datosFase4: {
      peti: form.peti,
      vigenciaPeti: form.vigenciaPeti || null,
      estrategiaPeti: form.estrategiaPeti || null,
    },
  datosFase5: {
    furag: form.furag || {},
  },
  datosFase6: {
    riesgos: form.riesgosIniciales || [],
  },
  datosFase7: {
    documentos: {},
  },
  ultimoGuardado: new Date().toISOString(),
});

const ProjectOnboardingWizard = ({
  project,
  saving = false,
  error = '',
  onComplete,
  completionDraft = null,
}) => {
  const canEditFechaRegistro = usePermission('PROYECTO:EDITAR_FECHA_REGISTRO');
  const [savedState] = useState(() => loadSavedState(project));
  const [step, setStep] = useState(savedState?.step || 1);
  const [form, setForm] = useState(() => savedState?.form || initialForm(project));
  const [errors, setErrors] = useState({});
  const [petiCatalog, setPetiCatalog] = useState(null);
  const [petiCatalogLoading, setPetiCatalogLoading] = useState(false);
  const [dependencias, setDependencias] = useState([]);
  const [entregableFiles, setEntregableFiles] = useState({});
  const [spellingErrors, setSpellingErrors] = useState(0);
  
  // Backend draft states
  const [fasesCompletadas, setFasesCompletadas] = useState({});
  const [loadingDraft, setLoadingDraft] = useState(false);
  const [savingDraft, setSavingDraft] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);
  const isInitializedRef = useRef(false);

  useEffect(() => {
    let active = true;
    const loadDependencias = async () => {
      try {
        const data = await configCatalogService.listarValoresParametrica('DEPENDENCIA');
        if (active && Array.isArray(data)) {
          setDependencias(data);
        }
      } catch {
        if (active) setDependencias([]);
      }
    };
    loadDependencias();
    return () => { active = false; };
  }, []);

  // Load draft from backend on mount
  useEffect(() => {
    let active = true;
    const loadBackendDraft = async () => {
      if (!project?.id) return;
      
      setLoadingDraft(true);
      try {
        const response = await projectService.getCompletionDraft(project.id);
        const draft = response?.data?.data ?? response?.data ?? response;
        
        console.log('[DRAFT] Response completa:', response);
        console.log('[DRAFT] Draft parseado:', draft);
        
        if (!active || !draft) return;
        
        // Restore form data from draft
        const restoredForm = { ...initialForm(project) };
        
        if (draft.datosFase1) {
          restoredForm.dependencia = draft.datosFase1.dependencia || restoredForm.dependencia;
          restoredForm.fechaInicio = draft.datosFase1.fechaInicio || restoredForm.fechaInicio;
          restoredForm.alcanceDetallado = draft.datosFase1.alcance || restoredForm.alcanceDetallado;
          restoredForm.presupuestoEstimado = draft.datosFase1.presupuestoEstimado || restoredForm.presupuestoEstimado;
          restoredForm.objetivosEspecificos = draft.datosFase1.objetivosEspecificos || restoredForm.objetivosEspecificos;
        }
        
        if (draft.datosFase2) {
          restoredForm.patrocinador = draft.datosFase2.patrocinador || restoredForm.patrocinador;
          restoredForm.equipoTrabajo = draft.datosFase2.equipoTrabajo || restoredForm.equipoTrabajo;
          restoredForm.stakeholders = draft.datosFase2.stakeholders || restoredForm.stakeholders;
        }
        
        if (draft.datosFase3) {
          restoredForm.fases = draft.datosFase3.fases || restoredForm.fases;
        }
        
        if (draft.datosFase4) {
          restoredForm.peti = draft.datosFase4.peti ?? restoredForm.peti;
          restoredForm.vigenciaPeti = draft.datosFase4.vigenciaPeti || restoredForm.vigenciaPeti;
          restoredForm.estrategiaPeti = draft.datosFase4.estrategiaPeti || restoredForm.estrategiaPeti;
        }
        
        if (draft.datosFase5) {
          restoredForm.furag = draft.datosFase5.furag || restoredForm.furag;
        }
        
        if (draft.datosFase6) {
          restoredForm.riesgosIniciales = draft.datosFase6.riesgos || restoredForm.riesgosIniciales;
        }
        
        console.log('[DRAFT] Form restaurado:', restoredForm);
        console.log('[DRAFT] Fases completadas:', draft.fasesCompletadas);
        console.log('[DRAFT] Paso a restaurar:', draft.faseActual);
        
        setForm(restoredForm);
        setFasesCompletadas(draft.fasesCompletadas || {});
        
        // Restore step from draft or localStorage
        const targetStep = draft.faseActual || savedState?.step || 1;
        setStep(targetStep);
        
        isInitializedRef.current = true;
      } catch (err) {
        console.error('Error cargando borrador del backend:', err);
        // Fallback to localStorage state
        isInitializedRef.current = true;
      } finally {
        if (active) setLoadingDraft(false);
      }
    };
    
    loadBackendDraft();
    return () => { active = false; };
  }, [project?.id]);

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

  const handleEntregableFileChange = (fIndex, hIndex, eIndex, file) => {
    const key = `${fIndex}-${hIndex}-${eIndex}`;
    setEntregableFiles((prev) => ({
      ...prev,
      [key]: file,
    }));
  };

  const isRetroactiveDate = (fechaLimite) => {
    if (!fechaLimite) return false;
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const fecha = new Date(`${fechaLimite}T00:00:00`);
    return fecha < hoy;
  };

  const validateStep = (targetStep, source = form) => {
    const nextErrors = {};

    if (targetStep === 1) {
      if (!source.dependencia) nextErrors.dependencia = 'Seleccione una dependencia.';
      if (!String(source.alcanceDetallado || '').trim()) nextErrors.alcanceDetallado = 'El alcance es obligatorio.';
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
            if (entregable.fechaInicio && entregable.fechaLimite && new Date(entregable.fechaLimite) < new Date(entregable.fechaInicio)) {
              nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_fechaLimite`] = 'La fecha limite debe ser mayor o igual a la fecha de inicio.';
            }
            if (entregable.fechaLimite && isRetroactiveDate(entregable.fechaLimite)) {
              const fileKey = `${faseIndex}-${hitoIndex}-${entregableIndex}`;
              if (!entregableFiles[fileKey]) {
                nextErrors[`ent_${faseIndex}_${hitoIndex}_${entregableIndex}_archivo`] = 'Debes adjuntar un archivo de soporte porque la fecha limite es anterior a hoy.';
              }
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
    }

    if (targetStep === 5) {
      // Validate using dynamic catalog keys — not hardcoded ones
      const preguntasFurag = petiCatalog?.furagPreguntas || [];
      if (preguntasFurag.length === 0) {
        nextErrors.furag = 'No se pudieron cargar las preguntas FURAG.';
      } else {
        preguntasFurag.forEach((p) => {
          const qKey = p?.key || p?.codigo || p?.id;
          if (qKey && !normalizeFuragValue(source.furag?.[qKey])) {
            nextErrors[`furag_${qKey}`] = 'Debe seleccionar una respuesta.';
          }
        });
      }
    }

    if (targetStep === 6) {
      const riesgos = source.riesgosIniciales || [];
      if (riesgos.length < 2) {
        nextErrors.riesgosIniciales = 'Debe registrar al menos 2 riesgos en la matriz de riesgos.';
      }
      riesgos.forEach((r, i) => {
        if (!r.descripcion?.trim()) nextErrors[`riesgo_${i}_descripcion`] = 'La descripcion del riesgo es obligatoria.';
        if (!r.probabilidad) nextErrors[`riesgo_${i}_probabilidad`] = 'La probabilidad es obligatoria.';
        if (!r.impacto) nextErrors[`riesgo_${i}_impacto`] = 'El impacto es obligatorio.';
      });
    }

    if (targetStep === 7) {
      if (!source.viabilizacionPdf) nextErrors.viabilizacionPdf = 'El documento de viabilidad es obligatorio.';
      if (!source.planComunicacionesPdf) nextErrors.planComunicacionesPdf = 'El Plan de Comunicaciones es obligatorio.';
    }

    return nextErrors;
  };

  const getFirstValidationMessage = (nextErrors) => {
    if (nextErrors.fases) return nextErrors.fases;
    const firstKey = Object.keys(nextErrors)[0];
    return firstKey ? nextErrors[firstKey] : 'Revise los campos marcados.';
  };

  const handleNext = async () => {
    const nextErrors = validateStep(step);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    
    const newFasesCompletadas = { ...fasesCompletadas, [step]: true };
    setFasesCompletadas(newFasesCompletadas);
    
    try {
      setSavingDraft(true);
      const nextStep = Math.min(step + 1, STEPS.length);
      const draftData = buildDraftPayload(nextStep, form, newFasesCompletadas);
      console.log('[SAVE] Guardando borrador:', JSON.stringify(draftData, null, 2));
      // Guardar borrador PRIMERO, luego marcar fase (secuencial para evitar race condition)
      await projectService.saveCompletionDraft(project.id, draftData);
      await projectService.completePhase(project.id, step);
      saveState(project, nextStep, form);
    } catch (err) {
      console.error('Error guardando borrador/marcando fase:', err);
      saveState(project, step + 1, form);
    } finally {
      setSavingDraft(false);
    }
    
    setStep((current) => Math.min(current + 1, STEPS.length));
  };

  const handleCloseWizard = () => {
    setShowCloseConfirm(true);
  };

  const confirmCloseWizard = async () => {
    setShowCloseConfirm(false);
    // Save draft before closing
    try {
      setSavingDraft(true);
      const draftData = buildDraftPayload(step, form, fasesCompletadas);
      await projectService.saveCompletionDraft(project.id, draftData);
      saveState(project, step, form);
    } catch (err) {
      console.error('Error guardando borrador al cerrar:', err);
      saveState(project, step, form);
    } finally {
      setSavingDraft(false);
    }
    clearSavedState(project);
    window.location.href = '/projects';
  };

  const cancelCloseWizard = () => {
    setShowCloseConfirm(false);
  };

  const buildPayload = () => {
    // Build the FURAG respuestas map dynamically from catalog keys.
    // To prevent validation errors on the backend due to fuzzy-matching mismatches
    // (such as "digitalizacionOAutomatizacion" not containing "digitalizacionautomatizacion" because of the "O"),
    // we map the catalog question keys to the exact hardcoded keys expected by the backend
    // whenever a match is detected.
    const catalogPreguntas = petiCatalog?.furagPreguntas || [];
    const furagRespuestas = {};
    
    const backendKeys = [
      'infraestructuraDatos',
      'interoperabilidad',
      'digitalizacionAutomatizacion',
      'contratacionPublica',
      'serviciosNube',
      'sandbox',
      'tecnologiasEmergentes'
    ];

    const normalizeString = (s) => String(s || '').toLowerCase().replace(/[^a-z0-9]/g, '');

    catalogPreguntas.forEach((p) => {
      const qKey = p?.key || p?.codigo || p?.id;
      if (!qKey) return;

      const userValue = normalizeFuragValue(form.furag?.[qKey]) ?? null;
      const normalizedQKey = normalizeString(qKey);

      // Check if this catalog question maps to one of our 7 standard backend fields
      let matchedBackendKey = null;
      for (const bKey of backendKeys) {
        const normalizedBKey = normalizeString(bKey);
        // Match if keys are equal, or if the question key contains the backend key,
        // or if we do a smart match for common variations (e.g. digitalizacion/automatizacion)
        if (
          normalizedQKey.includes(normalizedBKey) ||
          normalizedBKey.includes(normalizedQKey) ||
          (bKey === 'digitalizacionAutomatizacion' && normalizedQKey.includes('digitalizacion') && normalizedQKey.includes('automatizacion'))
        ) {
          matchedBackendKey = bKey;
          break;
        }
      }

      // If it maps to a standard field, send it under the exact expected key.
      // Otherwise, send it with its original catalog key.
      if (matchedBackendKey) {
        furagRespuestas[matchedBackendKey] = userValue;
      } else {
        furagRespuestas[qKey] = userValue;
      }
    });

    // Verify that all 7 required backend keys are populated. If any was missed by the
    // catalog map, assign it its value using a looser fallback check on form.furag.
    backendKeys.forEach((bKey) => {
      if (furagRespuestas[bKey] == null) {
        // Fallback search in form.furag keys
        const foundVal = getFuragValue(form.furag, bKey);
        if (foundVal != null) {
          furagRespuestas[bKey] = normalizeFuragValue(foundVal);
        }
      }
    });

    return {
      dependencia: form.dependencia,
      fechaInicio: form.fechaInicio,
      alcanceDetallado: String(form.alcanceDetallado || '').trim(),
      presupuestoEstimado: Number(form.presupuestoEstimado) || 0,
      objetivosEspecificos: (form.objetivosEspecificos || []).map((value) => value?.trim()).filter(Boolean),
      patrocinador: {
        nombre: form.patrocinador?.nombre || '',
        cargo: form.patrocinador?.cargo || '',
        procesoSigc: form.patrocinador?.procesoSigc || null,
        procedimientoSigc: form.patrocinador?.procedimientoSigc || null,
      },
      equipoTrabajo: (form.equipoTrabajo || []).map((member) => ({
        nombre: member.nombre,
        cargo: member.cargo,
        rol: member.rol,
        dependencia: member.dependencia || null,
        telefono: member.telefono || null,
        correo: member.correo || null,
      })),
      stakeholders: (form.stakeholders || []).map((s) => ({
        rol: s.rol || null,
        descripcion: s.descripcion || null,
        interes: s.interes || null,
        impacto: s.impacto || null,
      })),
      fases: (form.fases || []).map((fase, fIndex) => ({
        nombre: fase.nombre,
        descripcion: fase.descripcion || null,
        ponderacion: parseFloat(fase.ponderacion),
        hitos: (fase.hitos || []).map((hito, hIndex) => ({
          nombre: hito.nombre,
          descripcion: hito.descripcion || null,
          ponderacion: parseFloat(hito.ponderacion),
          entregables: (hito.entregables || []).map((entregable, eIndex) => {
            const fileKey = `${fIndex}-${hIndex}-${eIndex}`;
            const file = entregableFiles[fileKey];
            return {
              nombre: entregable.nombre,
              ponderacion: parseFloat(entregable.ponderacion),
              fechaInicio: entregable.fechaInicio,
              fechaLimite: entregable.fechaLimite,
              archivoPdf: file ? file.name : null,
            };
          }),
        })),
      })),
      peti: form.peti,
      vigenciaPeti: form.peti === true ? form.vigenciaPeti : null,
      estrategiaPeti: form.peti === true ? form.estrategiaPeti : null,
      tienePlanComunicaciones: true,
      // FuragDTO expects { respuestas: Map<String, RespuestaFurag> }
      furag: { respuestas: furagRespuestas },
      riesgosIniciales: (form.riesgosIniciales || []).map((r) => ({
        descripcion: r.descripcion || '',
        probabilidad: r.probabilidad || 'TRES',
        impacto: r.impacto || 'TRES',
        tratamiento: r.tratamiento || null,
        entidadResponsable: r.entidadResponsable || '',
        accionesMitigacion: r.accionesMitigacion || null,
        fechaAccion: r.fechaAccion || null,
      })),
    };
  };


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
    const payload = buildPayload();
    console.log('[FURAG] Body enviado al backend:', JSON.stringify(payload.furag, null, 2));
    console.log('[PAYLOAD COMPLETO]', JSON.stringify(payload, null, 2));
    onComplete?.(payload, {
      viabilizacionPdf: form.viabilizacionPdf || null,
      actaConstitucionPdf: form.actaConstitucionPdf || null,
      cronogramaPdf: form.cronogramaPdf || null,
      planComunicacionesPdf: form.planComunicacionesPdf || null,
      entregableFiles,
    });
  };

  const renderStep = () => {
    if (step === 1) {
      return (
        <div className="step-form">
          <h3 className="step-title">Datos complementarios</h3>
          <div className="form-grid">
            <div className="form-group">
              <label className="form-label">Dependencia y/o Secretaria Responsable *</label>
              <AutocompleteSelect
                value={form.dependencia || ''}
                onChange={(val) => handleChange({ dependencia: val })}
                options={dependencias.map((d) => ({ value: d, label: d }))}
                placeholder="Seleccione una dependencia"
                allLabel=""
                allValue=""
                className={errors.dependencia ? 'input-error' : ''}
              />
              {errors.dependencia && <span className="error-text">{errors.dependencia}</span>}
            </div>

            <div className="form-group">
              <label className="form-label">Fecha de Registro</label>
              <input
                type="date"
                className={`form-input ${canEditFechaRegistro ? '' : 'form-input-muted'}`}
                value={form.fechaInicio || ''}
                readOnly={!canEditFechaRegistro}
                onChange={canEditFechaRegistro ? (event) => handleChange({ fechaInicio: event.target.value }) : undefined}
              />
            </div>

            <div className="form-group">
              <label className="form-label">Presupuesto estimado *</label>
              <input
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                min="0"
                className={`form-input ${errors.presupuestoEstimado ? 'input-error' : ''}`}
                value={form.presupuestoEstimado}
                onChange={(event) => {
                  const onlyDigits = event.target.value.replace(/[^0-9]/g, '');
                  handleChange({ presupuestoEstimado: onlyDigits });
                }}
                placeholder="0"
              />
              {errors.presupuestoEstimado && <span className="error-text">{errors.presupuestoEstimado}</span>}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Alcance *</label>
            <SpellCheckInput
              as="textarea"
              className={`form-input form-textarea ${errors.alcanceDetallado ? 'input-error' : ''}`}
              value={form.alcanceDetallado || ''}
              onChange={(event) => handleChange({ alcanceDetallado: event.target.value })}
              onErrorChange={(hasError) => setSpellingErrors(prev => hasError ? prev + 1 : Math.max(0, prev - 1))}
              rows={5}
              placeholder="Describa alcance, limites y resultados esperados."
            />
            {errors.alcanceDetallado && <span className="error-text">{errors.alcanceDetallado}</span>}
          </div>

          <div className="form-group">
            <label className="form-label">Objetivos especificos</label>
            {(form.objetivosEspecificos || []).map((objective, index) => (
              <div key={`objective-${index}`} className="array-field-row">
                <SpellCheckInput
                  className="form-input"
                  value={objective}
                  onChange={(event) => handleObjetivoChange(index, event.target.value)}
                  onErrorChange={(hasError) => setSpellingErrors(prev => hasError ? prev + 1 : Math.max(0, prev - 1))}
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
      console.log('[RENDER Paso2] form.patrocinador:', form.patrocinador);
      console.log('[RENDER Paso2] form.equipoTrabajo:', form.equipoTrabajo);
      return <Paso2PatrocinadorEquipo data={form} onChange={handleChange} errors={errors} />;
    }

    if (step === 3) {
      return (
        <Paso3FasesHitosEntregables
          data={form}
          onChange={handleChange}
          errors={errors}
          allowEmpty={false}
          onFileChange={handleEntregableFileChange}
          pendingFiles={entregableFiles}
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
      return <Paso5Furag data={form} onChange={handleChange} errors={errors} preguntas={petiCatalog?.furagPreguntas} />;
    }

    if (step === 6) {
      return <Paso6MatrizRiesgos data={form} onChange={handleChange} errors={errors} />;
    }

    return <Paso7GestionDocumental data={form} onChange={handleChange} errors={errors} />;
  };

  return (
    <div className="project-onboarding">
      <div className="project-onboarding__shell">
        <form className="project-onboarding__panel card-surface" onSubmit={handleSubmit}>
          <div className="project-onboarding__header project-onboarding__hero">
            <div className="project-onboarding__hero-copy">
              <span className="modal-flow-badge">Momento {step} - Director de Proyecto</span>
              <h2 className="page-title">Completar informacion del proyecto</h2>
              <p className="page-subtitle">
                El proyecto fue registrado por el Gestor. Complete la informacion pendiente para habilitar los modulos operativos.
              </p>
            </div>
            <div className="project-onboarding__status-card">
              <span>Estado actual</span>
              <strong>Pendiente de Completar</strong>
              <small>Los modulos operativos siguen bloqueados hasta guardar este asistente.</small>
              {savingDraft && (
                <small className="project-onboarding__saving-indicator">
                  Guardando borrador...
                </small>
              )}
            </div>
            <button
              type="button"
              className="btn-ghost project-onboarding__close-btn"
              onClick={handleCloseWizard}
              title="Cerrar y guardar progreso"
            >
              <X size={18} />
            </button>
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
                className={`project-onboarding__step ${step === item.id ? 'active' : ''} ${fasesCompletadas[item.id] ? 'completed' : ''}`}
                onClick={() => {
                  if (item.id <= step) setStep(item.id);
                }}
              >
                {fasesCompletadas[item.id] ? (
                  <Check size={14} className="step-check" />
                ) : null}
                {String(item.id).padStart(2, '0')} {item.label}
              </button>
            ))}
          </div>

          {renderStep()}

          {Object.keys(errors).length > 0 && (
            <div className="error-banner">
              <AlertTriangle size={18} />
              <span>
                {errors.furag || getFirstValidationMessage(errors)}
              </span>
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

      {/* Close confirmation modal */}
      {showCloseConfirm && (
        <div className="modal-overlay" onClick={cancelCloseWizard}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h3>Cerrar asistente</h3>
            <p>
              Su progreso se ha guardado automaticamente. Puede continuar donde se quedo 
              la proxima vez que abra este proyecto.
            </p>
            <div className="modal-actions">
              <button type="button" className="btn-ghost" onClick={cancelCloseWizard}>
                Continuar editando
              </button>
              <button type="button" className="btn-primary" onClick={confirmCloseWizard}>
                Cerrar y guardar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProjectOnboardingWizard;
