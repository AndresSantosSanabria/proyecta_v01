import { useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation, useParams } from 'react-router-dom';
import { LoaderCircle } from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';
import { emitToast } from '../../utils/feedback';

const unwrapPayload = (value) => value?.data?.data ?? value?.data ?? value;

const NEEDS_COMPLETION_STATUSES = ['PENDIENTE_COMPLETAR', 'PENDIENTE', 'REGISTRADO'];

const projectNeedsCompletion = (projectData, statusData) => {
  if (statusData?.requiereCompletitud) return true;
  const projectStatus = String(projectData?.estado || '').toUpperCase();
  return NEEDS_COMPLETION_STATUSES.includes(projectStatus);
};

const ProjectLifecycleGuard = () => {
  const { id, codigoProyecto } = useParams();
  const location = useLocation();
  const projectId = useMemo(() => String(id || codigoProyecto || '').trim(), [codigoProyecto, id]);
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState(null);
  const [completionStatus, setCompletionStatus] = useState(null);
  const [completionDraft, setCompletionDraft] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;

    const loadLifecycle = async () => {
      if (!projectId) {
        setError('El proyecto solicitado no es valido.');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError('');

        const projectResponse = await projectService.getById(projectId);
        if (!active) return;
        const projectData = unwrapPayload(projectResponse);
        setProject(projectData);

        let statusData = null;
        try {
          const statusResponse = await projectService.getCompletionStatus(projectId);
          if (!active) return;
          statusData = unwrapPayload(statusResponse);
          setCompletionStatus(statusData);
        } catch {
          if (!active) return;
          setCompletionStatus(null);
        }

        if (projectNeedsCompletion(projectData, statusData)) {
          try {
            const draftResponse = await projectService.getCompletionDraft(projectId, { suppressAuthToast: true });
            if (!active) return;
            const draftData = unwrapPayload(draftResponse);
            setCompletionDraft(draftData);
          } catch {
            if (!active) return;
            setCompletionDraft(null);
          }
        } else {
          setCompletionDraft(null);
        }
      } catch (fetchError) {
        if (!active) return;
        console.error('No fue posible cargar el ciclo de vida del proyecto:', fetchError);
        setError('No fue posible cargar el proyecto.');
      } finally {
        if (active) setLoading(false);
      }
    };

    void loadLifecycle();

    return () => {
      active = false;
    };
  }, [projectId]);

  const needsCompletion = useMemo(() => {
    return projectNeedsCompletion(project, completionStatus);
  }, [completionStatus, project]);

  const puedeCompletar = useMemo(() => {
    if (completionStatus?.puedeCompletar !== undefined) return completionStatus.puedeCompletar;
    return needsCompletion;
  }, [completionStatus, needsCompletion]);

  const sanitizeBackendError = (raw) => {
    if (!raw) return 'No fue posible completar la informacion inicial.';
    if (/JDBC|SQL|column.*does not exist|PSQLException/i.test(raw)) {
      return 'Ocurrio un error interno del servidor. Verifique los datos del PETI e intente de nuevo. Si el problema persiste, contacte al administrador.';
    }
    if (/numeric field overflow/i.test(raw)) {
      return 'El valor del presupuesto excede el limite permitido. Verifique el monto e intente de nuevo.';
    }
    return raw;
  };

  const uploadDocuments = async (projectId, documents) => {
    const tipoMap = {
      viabilizacionPdf: 'VIABILIZACION',
      actaConstitucionPdf: 'ACTA_CONSTITUCION',
      cronogramaPdf: 'CRONOGRAMA',
      planComunicacionesPdf: 'PLAN_COMUNICACIONES',
    };

    const uploadErrors = [];

    for (const [key, file] of Object.entries(documents)) {
      if (!file) continue;
      const tipo = tipoMap[key];
      if (!tipo) continue;
      try {
        await documentService.cargarDocumento(projectId, tipo, file, 'Documento cargado durante el proceso de completar información del proyecto');
      } catch (docError) {
        const detail = docError?.response?.data?.detail
          || docError?.response?.data?.message
          || docError?.message
          || `Error desconocido subiendo ${tipo}`;
        console.error(`Error subiendo ${tipo}:`, docError);
        uploadErrors.push(`${tipo}: ${detail}`);
      }
    }

    if (uploadErrors.length > 0) {
      throw new Error(`Error al cargar documentos: ${uploadErrors.join('; ')}`);
    }
  };

  const uploadRetroactiveEvidence = async (projectId, projectResponse, entregableFiles) => {
    if (!entregableFiles || Object.keys(entregableFiles).length === 0) return;

    const fases = projectResponse?.fases || [];
    const flatEntregables = [];
    fases.forEach((fase, fIdx) => {
      (fase.hitos || []).forEach((hito, hIdx) => {
        (hito.entregables || []).forEach((entregable, eIdx) => {
          flatEntregables.push({ key: `${fIdx}-${hIdx}-${eIdx}`, id: entregable.id });
        });
      });
    });

    const errors = [];
    const hoy = new Date().toISOString().split('T')[0];

    for (const [fileKey, file] of Object.entries(entregableFiles)) {
      if (!file) continue;
      const match = flatEntregables.find((e) => e.key === fileKey);
      if (!match) {
        console.warn(`No se encontro entregable para la clave ${fileKey}`);
        continue;
      }
      try {
        await projectService.uploadEvidencia(projectId, match.id, file, hoy, () => {});
      } catch (err) {
        const detail = err?.response?.data?.detail || err?.message || 'Error subiendo evidencia';
        errors.push(`Entregable ${match.id}: ${detail}`);
      }
    }

    if (errors.length > 0) {
      console.warn('Algunas evidencias retroactivas no se pudieron subir:', errors);
    }
  };

  const handleComplete = async (payload, documents = {}) => {
    if (!projectId) return;

    setSaving(true);
    setError('');

    try {
      await uploadDocuments(projectId, documents);

      const response = await projectService.completeInitialInfo(projectId, payload);
      const updatedProject = unwrapPayload(response);
      setProject(updatedProject);

      if (documents.entregableFiles && Object.keys(documents.entregableFiles).length > 0) {
        await uploadRetroactiveEvidence(projectId, updatedProject, documents.entregableFiles);
      }

      setCompletionStatus((current) => ({
        ...(current || {}),
        requiereCompletitud: false,
        puedeCompletar: false,
      }));

      emitToast({
        tone: 'success',
        title: 'Proyecto completado',
        message: 'La información inicial quedó guardada y los módulos operativos ya pueden habilitarse.',
      });
    } catch (saveError) {
      console.error('No fue posible completar la informacion inicial:', saveError);
      const rawDetail = saveError?.response?.data?.detail
        || saveError?.response?.data?.message
        || saveError?.message
        || '';
      setError(sanitizeBackendError(rawDetail));
      emitToast({
        tone: 'error',
        title: 'No se pudo completar',
        message: sanitizeBackendError(rawDetail),
      });
    } finally {
      setSaving(false);
    }
  };

  if (!projectId) {
    return <Navigate to="/" replace state={{ from: location.pathname }} />;
  }

  if (loading) {
    return (
      <div className="screen-mobile-full" style={{ display: 'grid', placeItems: 'center' }}>
        <div className="section-stack" style={{ placeItems: 'center' }}>
          <LoaderCircle size={22} className="animate-spin" />
          <span className="muted-text">Cargando proyecto...</span>
        </div>
      </div>
    );
  }

  if (error && !needsCompletion) {
    return <div className="compact-page error-banner">{error}</div>;
  }

  if (needsCompletion && puedeCompletar) {
    return (
      <ProjectOnboardingWizard
        project={project}
        saving={saving}
        error={error}
        onComplete={handleComplete}
        completionDraft={completionDraft}
      />
    );
  }

  return <Outlet />;
};

export default ProjectLifecycleGuard;
