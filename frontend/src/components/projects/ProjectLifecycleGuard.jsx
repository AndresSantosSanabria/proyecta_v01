import { useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation, useParams } from 'react-router-dom';
import { LoaderCircle } from 'lucide-react';
import projectService from '../../services/projectService';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';

const unwrapPayload = (value) => value?.data?.data ?? value?.data ?? value;

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
        const [projectResponse, statusResponse] = await Promise.all([
          projectService.getById(projectId),
          projectService.getCompletionStatus(projectId),
        ]);

        if (!active) return;
        setProject(unwrapPayload(projectResponse));
        setCompletionStatus(unwrapPayload(statusResponse));
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

  const handleComplete = async (payload) => {
    if (!projectId) return;

    setSaving(true);
    setError('');

    try {
      const response = await projectService.completeInitialInfo(projectId, payload);
      setProject(unwrapPayload(response));
      setCompletionStatus((current) => ({
        ...(current || {}),
        requiereCompletitud: false,
        puedeCompletar: false,
      }));
    } catch (saveError) {
      console.error('No fue posible completar la informacion inicial:', saveError);
      const detail = saveError?.response?.data?.detail
        || saveError?.response?.data?.message
        || saveError?.message
        || 'No fue posible completar la informacion inicial.';
      setError(detail);
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

  if (error && !completionStatus?.requiereCompletitud) {
    return <div className="compact-page error-banner">{error}</div>;
  }

  if (completionStatus?.requiereCompletitud && completionStatus?.puedeCompletar) {
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
