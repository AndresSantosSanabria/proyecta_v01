import { useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation, useParams } from 'react-router-dom';
import { LoaderCircle } from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';

const unwrapPayload = (value) => value?.data?.data ?? value?.data ?? value;

const NEEDS_COMPLETION_STATUSES = ['PENDIENTE_COMPLETAR', 'PENDIENTE', 'REGISTRADO'];

const ProjectLifecycleGuard = () => {
  const { id, codigoProyecto } = useParams();
  const location = useLocation();
  const projectId = useMemo(() => String(id || codigoProyecto || '').trim(), [codigoProyecto, id]);
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState(null);
  const [completionStatus, setCompletionStatus] = useState(null);
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

        try {
          const statusResponse = await projectService.getCompletionStatus(projectId);
          if (!active) return;
          setCompletionStatus(unwrapPayload(statusResponse));
        } catch {
          if (!active) return;
          setCompletionStatus(null);
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
    if (completionStatus?.requiereCompletitud) return true;
    const projectStatus = String(project?.estado || '').toUpperCase();
    return NEEDS_COMPLETION_STATUSES.includes(projectStatus);
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
        await documentService.cargarDocumento(projectId, tipo, file);
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

  const handleComplete = async (payload, documents = {}) => {
    if (!projectId) return;

    setSaving(true);
    setError('');

    try {
      await uploadDocuments(projectId, documents);

      const response = await projectService.completeInitialInfo(projectId, payload);
      setProject(unwrapPayload(response));

      setCompletionStatus((current) => ({
        ...(current || {}),
        requiereCompletitud: false,
        puedeCompletar: false,
      }));
    } catch (saveError) {
      console.error('No fue posible completar la informacion inicial:', saveError);
      const rawDetail = saveError?.response?.data?.detail
        || saveError?.response?.data?.message
        || saveError?.message
        || '';
      setError(sanitizeBackendError(rawDetail));
    } finally {
      setSaving(false);
    }
  };

  if (!projectId) {
    return <Navigate to="/access-denied" replace state={{ from: location.pathname }} />;
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
      />
    );
  }

  return <Outlet />;
};

export default ProjectLifecycleGuard;
