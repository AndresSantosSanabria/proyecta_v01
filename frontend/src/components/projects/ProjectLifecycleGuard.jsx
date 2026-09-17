import { useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation, useParams } from 'react-router-dom';
import { LoaderCircle, Upload, FileCheck, AlertTriangle, CheckCircle, XCircle, Clock, Eye, Download, RotateCcw, AlertOctagon, X } from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import { useAuthContext } from '../../context/AuthContext';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';
import { emitToast } from '../../utils/feedback';

const unwrapPayload = (value) => value?.data?.data ?? value?.data ?? value;

const NEEDS_COMPLETION_STATUSES = ['PENDIENTE_COMPLETAR', 'PENDIENTE', 'REGISTRADO'];

const projectNeedsCompletion = (projectData, statusData) => {
  if (statusData?.requiereCompletitud) return true;
  const projectStatus = String(projectData?.estado || '').toUpperCase();
  return NEEDS_COMPLETION_STATUSES.includes(projectStatus);
};

const DocumentosPreWizardView = ({ project, completionStatus, onComplete, isResubmission = false }) => {
  const [files, setFiles] = useState({
    VIABILIZACION: null,
    PLAN_COMUNICACIONES: null,
    MATRIZ_RIESGOS_VIABILIDAD: null,
  });
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState({});

  const DOC_LABELS = {
    VIABILIZACION: 'Documento de Viabilidad',
    PLAN_COMUNICACIONES: 'Plan de Comunicaciones',
    MATRIZ_RIESGOS_VIABILIDAD: 'Matriz de Riesgos de Viabilidad',
  };

  const handleFileSelect = (field) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf';
    input.onchange = (e) => {
      const selected = e.target.files[0];
      if (selected) {
        setFiles((prev) => ({ ...prev, [field]: selected }));
      }
    };
    input.click();
  };

  const handleRemove = (field) => {
    setFiles((prev) => ({ ...prev, [field]: null }));
  };

  const handleDrop = (field, e) => {
    e.preventDefault();
    const dropped = e.dataTransfer.files[0];
    if (dropped && dropped.type === 'application/pdf') {
      setFiles((prev) => ({ ...prev, [field]: dropped }));
    }
  };

  const allFilesSelected = Object.values(files).every((f) => f !== null);

  const handleUploadAll = async () => {
    if (!allFilesSelected || !project?.id) return;
    setUploading(true);
    setUploadProgress({});

    try {
      for (const [tipo, file] of Object.entries(files)) {
        if (!file) continue;
        setUploadProgress((prev) => ({ ...prev, [tipo]: 'uploading' }));
        const observacion = isResubmission
          ? `Documento subsanado por el Director`
          : `Documento cargado durante el proceso de completar informacion del proyecto`;
        await documentService.cargarDocumento(project.id, tipo, file, observacion);
        setUploadProgress((prev) => ({ ...prev, [tipo]: 'done' }));
      }

      emitToast({
        tone: 'success',
        title: isResubmission ? 'Documentos subsanados' : 'Documentos cargados',
        message: isResubmission
          ? 'Los 3 documentos fueron cargados exitosamente. El Gestor sera notificado para su revision.'
          : 'Los 3 documentos fueron cargados exitosamente. El Gestor sera notificado para su verificacion.',
      });
      onComplete();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error subiendo documentos';
      emitToast({
        tone: 'error',
        title: 'Error al cargar',
        message: detail,
      });
    } finally {
      setUploading(false);
    }
  };

  const isDevuelta = completionStatus?.viabilidadEstado === 'DEVUELTA';

  return (
    <div className="project-onboarding">
      <div className="project-onboarding__shell">
        <div className="project-onboarding__panel card-surface">
          <div className="project-onboarding__header project-onboarding__hero">
            <div className="project-onboarding__hero-copy">
              <span className="modal-flow-badge">
                {isDevuelta ? 'Documentos Devueltos' : 'Paso 1 - Director de Proyecto'}
              </span>
              <h2 className="page-title">
                {isDevuelta ? 'Subsanar Documentos' : 'Cargar Documentos Pre-Wizard'}
              </h2>
              <p className="page-subtitle">
                {isDevuelta
                  ? 'El Gestor devolvio los documentos con observaciones. Por favor, subsane las observaciones y vuelva a cargar los 3 documentos.'
                  : 'Para continuar con la completitud del proyecto, primero debe cargar los 3 documentos requeridos. El Gestor verificara los documentos antes de que pueda acceder al wizard de completitud.'}
              </p>
            </div>
          </div>

          {isDevuelta && completionStatus?.viabilidadObservaciones && (
            <div className="error-banner" style={{ margin: '0 1.5rem', background: '#fff3cd', borderColor: '#ffc107', color: '#856404' }}>
              <AlertTriangle size={18} />
              <div>
                <strong>Observaciones del Gestor:</strong>
                <p style={{ margin: '0.5rem 0 0 0' }}>{completionStatus.viabilidadObservaciones}</p>
              </div>
            </div>
          )}

          {!isDevuelta && (
            <div className="project-onboarding__locked info-banner">
              <AlertTriangle size={16} />
              <div>
                <strong>Requisito previo</strong>
                <p>El boton "Completar Proyecto" se habilitara solo despues de que el Gestor verifique los 3 documentos.</p>
              </div>
            </div>
          )}

          <section className="project-onboarding__locked-grid" aria-label="Datos del proyecto">
            <article className="project-onboarding__locked-card">
              <span>Codigo</span>
              <strong>{project?.id || 'PENDIENTE'}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Proyecto</span>
              <strong>{project?.nombre || 'Proyecto sin nombre'}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Director asignado</span>
              <strong>{project?.director || 'Sin director'}</strong>
            </article>
          </section>

          <div className="step-form" style={{ padding: '1.5rem' }}>
            <h3 className="step-title">Documentos Requeridos</h3>
            <p className="help-text">
              Cargue los 3 documentos en formato PDF. Todos son obligatorios para continuar.
            </p>

            <div style={{ display: 'grid', gap: '1rem' }}>
              {Object.entries(DOC_LABELS).map(([tipo, label]) => {
                const file = files[tipo];
                const progress = uploadProgress[tipo];

                return (
                  <div
                    key={tipo}
                    className={`doc-card ${file ? 'has-file' : ''}`}
                    onDragOver={(e) => { e.preventDefault(); }}
                    onDrop={(e) => handleDrop(tipo, e)}
                    onClick={!file ? () => handleFileSelect(tipo) : undefined}
                    style={{ cursor: file ? 'default' : 'pointer' }}
                  >
                    {file ? (
                      <div className="doc-file-selected">
                        <div className="doc-file-icon">
                          {progress === 'done' ? (
                            <CheckCircle size={16} style={{ color: '#28a745' }} />
                          ) : (
                            <FileCheck size={16} strokeWidth={2.2} />
                          )}
                        </div>
                        <div className="doc-file-info">
                          <span className="doc-file-name">{file.name}</span>
                          <span className="doc-file-size">{(file.size / 1024).toFixed(1)} KB</span>
                        </div>
                        {!uploading && (
                          <button
                            type="button"
                            className="doc-file-remove"
                            onClick={(e) => { e.stopPropagation(); handleRemove(tipo); }}
                          >
                            <XCircle size={16} />
                          </button>
                        )}
                      </div>
                    ) : (
                      <div className="doc-card-placeholder">
                        <Upload size={24} />
                        <p>{label}</p>
                        <small>Arrastre o haga clic para seleccionar. Solo PDF, max 20 MB.</small>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
              <button
                type="button"
                className="btn-primary"
                onClick={handleUploadAll}
                disabled={!allFilesSelected || uploading}
              >
                {uploading ? (
                  <>
                    <LoaderCircle size={16} className="animate-spin" />
                    Subiendo documentos...
                  </>
                ) : (
                  <>
                    <Upload size={16} />
                    {isDevuelta ? 'Cargar Documentos Subsanados' : 'Cargar 3 Documentos'}
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const DocumentosCargadosView = ({ project, completionStatus, onRefresh }) => {
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectObservaciones, setRejectObservaciones] = useState('');
  const [loadingAction, setLoadingAction] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewName, setPreviewName] = useState('');
  const [downloading, setDownloading] = useState({});

  const { hasRole, isAdminLocal, transversal } = useAuthContext();
  const isGestor = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_PROYECTOS') || hasRole('GESTOR_TIC');

  const handlePreview = async (tipo, label) => {
    try {
      setDownloading((prev) => ({ ...prev, [tipo]: 'previewing' }));
      const blob = await documentService.descargarDocumento(project.id, tipo);
      const url = window.URL.createObjectURL(blob);
      setPreviewUrl(url);
      setPreviewName(label);
    } catch {
      emitToast({ tone: 'error', title: 'Error', message: 'No se pudo cargar el documento para visualizar.' });
    } finally {
      setDownloading((prev) => ({ ...prev, [tipo]: null }));
    }
  };

  const handleDownload = async (tipo, label) => {
    try {
      setDownloading((prev) => ({ ...prev, [tipo]: 'downloading' }));
      const blob = await documentService.descargarDocumento(project.id, tipo);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${label}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch {
      emitToast({ tone: 'error', title: 'Error', message: 'No se pudo descargar el documento.' });
    } finally {
      setDownloading((prev) => ({ ...prev, [tipo]: null }));
    }
  };

  const handleClosePreview = () => {
    if (previewUrl) window.URL.revokeObjectURL(previewUrl);
    setPreviewUrl(null);
    setPreviewName('');
  };

  const handleApprove = async () => {
    setLoadingAction(true);
    try {
      await projectService.aprobarViabilidad(project.id);
      emitToast({
        tone: 'success',
        title: 'Documentos aprobados',
        message: 'Los documentos fueron verificados. El Director puede ahora completar el proyecto.',
      });
      onRefresh?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error aprobando documentos';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setLoadingAction(false);
    }
  };

  const handleReject = async () => {
    if (!rejectObservaciones.trim()) {
      emitToast({ tone: 'warning', title: 'Observaciones requeridas', message: 'Debe ingresar observaciones para devolver los documentos.' });
      return;
    }
    setLoadingAction(true);
    try {
      await projectService.devolverViabilidad(project.id, rejectObservaciones.trim());
      emitToast({
        tone: 'success',
        title: 'Documentos devueltos',
        message: 'Los documentos fueron devueltos al Director con observaciones.',
      });
      setShowRejectModal(false);
      setRejectObservaciones('');
      onRefresh?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error devolviendo documentos';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setLoadingAction(false);
    }
  };

  return (
    <div className="project-onboarding">
      <div className="project-onboarding__shell">
        <div className="project-onboarding__panel card-surface">
          <div className="project-onboarding__header project-onboarding__hero">
            <div className="project-onboarding__hero-copy">
              <span className="modal-flow-badge" style={{ background: '#ffc107', color: '#856404' }}>
                Pendiente de Verificacion
              </span>
              <h2 className="page-title">Documentos Cargados</h2>
              <p className="page-subtitle">
                {isGestor
                  ? 'Los 3 documentos fueron cargados por el Director. Verifique los documentos y tome una decision.'
                  : 'Los 3 documentos fueron cargados y estan pendientes de verificacion por parte del Gestor.'}
              </p>
            </div>
          </div>

          <section className="project-onboarding__locked-grid" aria-label="Datos del proyecto">
            <article className="project-onboarding__locked-card">
              <span>Codigo</span>
              <strong>{project?.id || 'PENDIENTE'}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Proyecto</span>
              <strong>{project?.nombre || 'Proyecto sin nombre'}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Documentos</span>
              <strong style={{ color: '#ffc107' }}>3 documentos cargados - En revision</strong>
            </article>
          </section>

          <div className="step-form" style={{ padding: '1.5rem' }}>
            <h3 className="step-title">Documentos Cargados</h3>
            <div style={{ display: 'grid', gap: '0.75rem' }}>
              {[
                { tipo: 'VIABILIZACION', label: 'Documento de Viabilidad' },
                { tipo: 'PLAN_COMUNICACIONES', label: 'Plan de Comunicaciones' },
                { tipo: 'MATRIZ_RIESGOS_VIABILIDAD', label: 'Matriz de Riesgos de Viabilidad' },
              ].map(({ tipo, label }) => (
                <div key={tipo} className="doc-card has-file" style={{ cursor: 'default' }}>
                  <div className="doc-file-selected">
                    <div className="doc-file-icon">
                      <Clock size={16} style={{ color: '#ffc107' }} />
                    </div>
                    <div className="doc-file-info">
                      <span className="doc-file-name">{label}</span>
                      <span className="doc-file-size" style={{ color: '#ffc107' }}>Pendiente de revision</span>
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', marginLeft: 'auto' }}>
                      <button
                        type="button"
                        onClick={() => handlePreview(tipo, label)}
                        disabled={downloading[tipo] === 'previewing'}
                        style={{
                          display: 'inline-flex', alignItems: 'center', gap: '4px',
                          padding: '6px 10px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)',
                          background: 'rgba(59,130,246,0.15)', color: '#60a5fa', cursor: 'pointer',
                          fontSize: '12px', fontWeight: 600, transition: 'all 0.15s',
                        }}
                        title="Visualizar documento"
                      >
                        <Eye size={14} />
                        {downloading[tipo] === 'previewing' ? 'Abriendo...' : 'Ver'}
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDownload(tipo, label)}
                        disabled={downloading[tipo] === 'downloading'}
                        style={{
                          display: 'inline-flex', alignItems: 'center', gap: '4px',
                          padding: '6px 10px', borderRadius: '6px', border: '1px solid rgba(255,255,255,0.15)',
                          background: 'rgba(16,185,129,0.15)', color: '#34d399', cursor: 'pointer',
                          fontSize: '12px', fontWeight: 600, transition: 'all 0.15s',
                        }}
                        title="Descargar documento"
                      >
                        <Download size={14} />
                        {downloading[tipo] === 'downloading' ? 'Descargando...' : 'Descargar'}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {isGestor && (
              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
                <button
                  type="button"
                  className="btn-outline"
                  onClick={() => setShowRejectModal(true)}
                  disabled={loadingAction}
                >
                  <XCircle size={16} />
                  Devolver Documentos
                </button>
                <button
                  type="button"
                  className="btn-primary"
                  onClick={handleApprove}
                  disabled={loadingAction}
                >
                  {loadingAction ? (
                    <LoaderCircle size={16} className="animate-spin" />
                  ) : (
                    <CheckCircle size={16} />
                  )}
                  Aprobar Documentos
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {showRejectModal && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'grid', placeItems: 'center', zIndex: 1000 }}>
          <div className="card-surface" style={{ width: '90%', maxWidth: '480px', padding: '1.5rem' }}>
            <h3 style={{ margin: '0 0 1rem' }}>Devolver Documentos</h3>
            <p className="help-text" style={{ marginBottom: '1rem' }}>
              Ingrese las observaciones para que el Director subsane los documentos.
            </p>
            <textarea
              className="form-control"
              rows={4}
              placeholder="Describa las observaciones..."
              value={rejectObservaciones}
              onChange={(e) => setRejectObservaciones(e.target.value)}
              style={{ width: '100%', resize: 'vertical' }}
            />
            <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', marginTop: '1rem' }}>
              <button
                type="button"
                className="btn-outline"
                onClick={() => { setShowRejectModal(false); setRejectObservaciones(''); }}
                disabled={loadingAction}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn-primary"
                onClick={handleReject}
                disabled={loadingAction || !rejectObservaciones.trim()}
                style={{ background: '#dc3545' }}
              >
                {loadingAction ? <LoaderCircle size={16} className="animate-spin" /> : <XCircle size={16} />}
                Devolver
              </button>
            </div>
          </div>
        </div>
      )}

      {previewUrl && (
        <div
          style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'grid', placeItems: 'center', zIndex: 1100 }}
          onClick={handleClosePreview}
        >
          <div
            style={{ width: '90vw', height: '90vh', maxWidth: '1100px', background: '#1e1e2e', borderRadius: '12px', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem 1rem', borderBottom: '1px solid rgba(255,255,255,0.1)' }}>
              <span style={{ color: '#e2e8f0', fontWeight: 600, fontSize: '14px' }}>{previewName}</span>
              <button
                onClick={handleClosePreview}
                style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer', padding: '4px' }}
              >
                <X size={20} />
              </button>
            </div>
            <iframe
              src={previewUrl}
              style={{ flex: 1, border: 'none', width: '100%' }}
              title={previewName}
            />
          </div>
        </div>
      )}
    </div>
  );
};

const PlazoVencidoView = ({ project, completionStatus, onRefresh }) => {
  const [loadingAction, setLoadingAction] = useState(false);
  const { hasRole, isAdminLocal, transversal } = useAuthContext();
  const isGestor = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_PROYECTOS') || hasRole('GESTOR_TIC');

  const handleForceClose = async () => {
    if (!window.confirm('Esta seguro de cerrar forzosamente este proyecto? Esta accion no se puede deshacer.')) return;
    setLoadingAction(true);
    try {
      await projectService.cerrarForzoso(project.id);
      emitToast({
        tone: 'success',
        title: 'Proyecto cerrado',
        message: 'El proyecto fue cerrado forzosamente.',
      });
      onRefresh?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error cerrando proyecto';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setLoadingAction(false);
    }
  };

  return (
    <div className="project-onboarding">
      <div className="project-onboarding__shell">
        <div className="project-onboarding__panel card-surface">
          <div className="project-onboarding__header project-onboarding__hero">
            <div className="project-onboarding__hero-copy">
              <span className="modal-flow-badge" style={{ background: '#dc3545', color: 'white' }}>
                Plazo Vencido
              </span>
              <h2 className="page-title">Plazo de Completitud Vencido</h2>
              <p className="page-subtitle">
                El plazo de 30 dias para completar el proyecto ha vencido.
                {isGestor
                  ? ' Puede realizar el cierre forzoso del proyecto.'
                  : ' El Gestor debe decidir si realizar el cierre forzoso.'}
              </p>
            </div>
          </div>

          <section className="project-onboarding__locked-grid" aria-label="Datos del proyecto">
            <article className="project-onboarding__locked-card">
              <span>Codigo</span>
              <strong>{project?.id || 'PENDIENTE'}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Proyecto</span>
              <strong>{project?.nombre || 'Proyecto sin nombre'}</strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Fecha limite</span>
              <strong style={{ color: '#dc3545' }}>
                {completionStatus?.fechaLimiteCompletar || 'Sin fecha'}
              </strong>
            </article>
            <article className="project-onboarding__locked-card">
              <span>Director</span>
              <strong>{project?.director || 'Sin director'}</strong>
            </article>
          </section>

          {isGestor && (
            <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '1.5rem' }}>
              <button
                type="button"
                className="btn-primary"
                onClick={handleForceClose}
                disabled={loadingAction}
                style={{ background: '#dc3545' }}
              >
                {loadingAction ? (
                  <LoaderCircle size={16} className="animate-spin" />
                ) : (
                  <AlertOctagon size={16} />
                )}
                Cerrar Proyecto Forzosamente
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
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

  const refreshLifecycle = async () => {
    if (!projectId) return;
    try {
      setLoading(true);
      setError('');
      const projectResponse = await projectService.getById(projectId);
      const projectData = unwrapPayload(projectResponse);
      setProject(projectData);

      let statusData = null;
      try {
        const statusResponse = await projectService.getCompletionStatus(projectId);
        statusData = unwrapPayload(statusResponse);
        setCompletionStatus(statusData);
      } catch {
        setCompletionStatus(null);
      }

      if (projectNeedsCompletion(projectData, statusData)) {
        try {
          const draftResponse = await projectService.getCompletionDraft(projectId, { suppressAuthToast: true });
          const draftData = unwrapPayload(draftResponse);
          setCompletionDraft(draftData);
        } catch {
          setCompletionDraft(null);
        }
      } else {
        setCompletionDraft(null);
      }
    } catch (fetchError) {
      console.error('No fue posible cargar el ciclo de vida del proyecto:', fetchError);
      setError('No fue posible cargar el proyecto.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let active = true;
    const load = async () => {
      await refreshLifecycle();
      if (!active) return;
    };
    void load();
    return () => { active = false; };
  }, [projectId]);

  const needsCompletion = useMemo(() => {
    return projectNeedsCompletion(project, completionStatus);
  }, [completionStatus, project]);

  const viabilidadEstado = completionStatus?.viabilidadEstado || 'PENDIENTE';
  const viabilidadAprobada = completionStatus?.viabilidadAprobada || false;
  const puedeCargarViabilidad = completionStatus?.puedeCargarViabilidad || false;
  const puedeCompletarWizard = completionStatus?.puedeCompletarWizard || false;

  const puedeCompletar = useMemo(() => {
    if (puedeCompletarWizard) return true;
    if (completionStatus?.puedeCompletar !== undefined) return completionStatus.puedeCompletar;
    return needsCompletion;
  }, [completionStatus, needsCompletion, puedeCompletarWizard]);

  const sanitizeBackendError = (raw) => {
    if (!raw) return 'No fue posible completar la información inicial.';
    if (/JDBC|SQL|column.*does not exist|PSQLException/i.test(raw)) {
      return 'Ocurrió un error interno del servidor. Verifique los datos del PETI e intente de nuevo. Si el problema persiste, contacte al administrador.';
    }
    if (/numeric field overflow/i.test(raw)) {
      return 'El valor del presupuesto excede el límite permitido. Verifique el monto e intente de nuevo.';
    }
    return raw;
  };

  const uploadDocuments = async (projectId, documents) => {
    const tipoMap = {
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

  if (!needsCompletion) {
    return <Outlet />;
  }

  if (viabilidadEstado === 'PENDIENTE' && puedeCargarViabilidad) {
    return (
      <DocumentosPreWizardView
        project={project}
        completionStatus={completionStatus}
        onComplete={refreshLifecycle}
      />
    );
  }

  if (viabilidadEstado === 'DEVUELTA') {
    return (
      <DocumentosPreWizardView
        project={project}
        completionStatus={completionStatus}
        onComplete={refreshLifecycle}
        isResubmission={true}
      />
    );
  }

  if (viabilidadEstado === 'CARGADA') {
    return (
      <DocumentosCargadosView
        project={project}
        completionStatus={completionStatus}
        onRefresh={refreshLifecycle}
      />
    );
  }

  if (viabilidadAprobada && !puedeCompletar && completionStatus?.plazoVencido && !completionStatus?.cierreForzoso) {
    return (
      <PlazoVencidoView
        project={project}
        completionStatus={completionStatus}
        onRefresh={refreshLifecycle}
      />
    );
  }

  if (viabilidadAprobada && puedeCompletar) {
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
