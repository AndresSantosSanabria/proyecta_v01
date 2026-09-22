import { useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation, useParams } from 'react-router-dom';
import { LoaderCircle, Upload, FileCheck, AlertTriangle, CheckCircle, XCircle, Clock, Eye, Download, AlertOctagon, X } from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import { useAuthContext } from '../../context/AuthContext';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';
import { emitToast } from '../../utils/feedback';
import './ProjectLifecycleGuard.css';

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
          <div className="plg-hero">
            <span className={`plg-hero__badge ${isDevuelta ? 'plg-hero__badge--danger' : 'plg-hero__badge--warning'}`}>
              {isDevuelta ? 'Documentos Devueltos' : 'Paso 1 - Director de Proyecto'}
            </span>
            <h2 className="plg-hero__title">
              {isDevuelta ? 'Subsanar Documentos' : 'Cargar Documentos Pre-Wizard'}
            </h2>
            <p className="plg-hero__subtitle">
              {isDevuelta
                ? 'El Gestor devolvio los documentos con observaciones. Por favor, subsane las observaciones y vuelva a cargar los 3 documentos.'
                : 'Para continuar con la completitud del proyecto, primero debe cargar los 3 documentos requeridos. El Gestor verificara los documentos antes de que pueda acceder al wizard de completitud.'}
            </p>
          </div>

          {isDevuelta && completionStatus?.viabilidadObservaciones && (
            <div className="plg-observation">
              <AlertTriangle size={18} />
              <div>
                <p className="plg-observation__label">Observaciones del Gestor</p>
                <p className="plg-observation__text">{completionStatus.viabilidadObservaciones}</p>
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

          <section className="plg-info-grid" aria-label="Datos del proyecto">
            <article className="plg-info-card">
              <span className="plg-info-card__label">Codigo</span>
              <strong className="plg-info-card__value">{project?.id || 'PENDIENTE'}</strong>
            </article>
            <article className="plg-info-card">
              <span className="plg-info-card__label">Proyecto</span>
              <strong className="plg-info-card__value">{project?.nombre || 'Proyecto sin nombre'}</strong>
            </article>
            <article className="plg-info-card">
              <span className="plg-info-card__label">Director asignado</span>
              <strong className="plg-info-card__value">{project?.director || 'Sin director'}</strong>
            </article>
          </section>

          <section className="plg-docs-section">
            <div className="plg-docs-section__header">
              <h3 className="plg-docs-section__title">Documentos Requeridos</h3>
              <span className="plg-docs-section__count">3</span>
            </div>
            <p className="plg-docs-section__hint">
              Cargue los 3 documentos en formato PDF. Todos son obligatorios para continuar.
            </p>

            <div className="plg-doc-list">
              {Object.entries(DOC_LABELS).map(([tipo, label]) => {
                const file = files[tipo];
                const progress = uploadProgress[tipo];

                return (
                  <div
                    key={tipo}
                    className={`plg-doc-card ${file ? 'plg-doc-card--has-file' : 'plg-doc-card--empty'}`}
                    onDragOver={(e) => { e.preventDefault(); }}
                    onDrop={(e) => handleDrop(tipo, e)}
                    onClick={!file ? () => handleFileSelect(tipo) : undefined}
                  >
                    {file ? (
                      <div className="plg-doc-card__inner">
                        <div className={`plg-doc-card__icon ${progress === 'uploading' ? 'plg-doc-card__icon--uploading' : ''}`}>
                          {progress === 'done' ? (
                            <CheckCircle size={16} />
                          ) : progress === 'uploading' ? (
                            <LoaderCircle size={16} className="animate-spin" />
                          ) : (
                            <FileCheck size={16} strokeWidth={2.2} />
                          )}
                        </div>
                        <div className="plg-doc-card__info">
                          <span className="plg-doc-card__name">{file.name}</span>
                          <span className="plg-doc-card__size">
                            {progress === 'uploading' ? 'Subiendo...' : progress === 'done' ? 'Cargado' : `${(file.size / 1024).toFixed(1)} KB`}
                          </span>
                        </div>
                        {!uploading && (
                          <div className="plg-doc-card__actions">
                            <button
                              type="button"
                              className="plg-doc-card__btn plg-doc-card__btn--remove"
                              onClick={(e) => { e.stopPropagation(); handleRemove(tipo); }}
                              title="Quitar archivo"
                            >
                              <XCircle size={12} />
                            </button>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="plg-doc-card__placeholder">
                        <div className="plg-doc-card__placeholder-icon">
                          <Upload size={20} />
                        </div>
                        <p className="plg-doc-card__placeholder-title">{label}</p>
                        <p className="plg-doc-card__placeholder-hint">Arrastre o haga clic para seleccionar. Solo PDF, max 20 MB.</p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </section>

          <div className="plg-footer">
            <button
              type="button"
              className="plg-btn-primary"
              onClick={handleUploadAll}
              disabled={!allFilesSelected || uploading}
            >
              {uploading ? (
                <>
                  <LoaderCircle size={12} className="animate-spin" />
                  Subiendo...
                </>
              ) : (
                <>
                  <Upload size={12} />
                  {isDevuelta ? 'Subsanar Documentos' : 'Cargar Documentos'}
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const DocumentosCargadosView = ({ project, onRefresh }) => {
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
          <div className="plg-hero">
            <span className="plg-hero__badge plg-hero__badge--warning">
              Pendiente de Verificacion
            </span>
            <h2 className="plg-hero__title">Documentos Cargados</h2>
            <p className="plg-hero__subtitle">
              {isGestor
                ? 'Los 3 documentos fueron cargados por el Director. Verifique los documentos y tome una decision.'
                : 'Los 3 documentos fueron cargados y estan pendientes de verificacion por parte del Gestor.'}
            </p>
          </div>

          <section className="plg-info-grid" aria-label="Datos del proyecto">
            <article className="plg-info-card">
              <span className="plg-info-card__label">Codigo</span>
              <strong className="plg-info-card__value">{project?.id || 'PENDIENTE'}</strong>
            </article>
            <article className="plg-info-card">
              <span className="plg-info-card__label">Proyecto</span>
              <strong className="plg-info-card__value">{project?.nombre || 'Proyecto sin nombre'}</strong>
            </article>
            <article className="plg-info-card">
              <span className="plg-info-card__label">Estado</span>
              <strong className="plg-info-card__value plg-info-card__value--warning">3 documentos cargados - En revision</strong>
            </article>
          </section>

          <section className="plg-docs-section">
            <div className="plg-docs-section__header">
              <h3 className="plg-docs-section__title">Documentos Cargados</h3>
              <span className="plg-docs-section__count">3</span>
            </div>

            <div className="plg-doc-list">
              {[
                { tipo: 'VIABILIZACION', label: 'Documento de Viabilidad' },
                { tipo: 'PLAN_COMUNICACIONES', label: 'Plan de Comunicaciones' },
                { tipo: 'MATRIZ_RIESGOS_VIABILIDAD', label: 'Matriz de Riesgos de Viabilidad' },
              ].map(({ tipo, label }) => (
                <div key={tipo} className="plg-doc-card plg-doc-card--has-file">
                  <div className="plg-doc-card__inner">
                    <div className="plg-doc-card__icon">
                      <Clock size={16} />
                    </div>
                    <div className="plg-doc-card__info">
                      <span className="plg-doc-card__name">{label}</span>
                      <span className="plg-doc-card__size">Pendiente de revision</span>
                    </div>
                    <div className="plg-doc-card__actions">
                      <button
                        type="button"
                        className="plg-doc-card__btn plg-doc-card__btn--preview"
                        onClick={() => handlePreview(tipo, label)}
                        disabled={downloading[tipo] === 'previewing'}
                        title="Visualizar documento"
                      >
                        <Eye size={12} />
                        {downloading[tipo] === 'previewing' ? 'Abriendo...' : 'Ver'}
                      </button>
                      <button
                        type="button"
                        className="plg-doc-card__btn plg-doc-card__btn--download"
                        onClick={() => handleDownload(tipo, label)}
                        disabled={downloading[tipo] === 'downloading'}
                        title="Descargar documento"
                      >
                        <Download size={12} />
                        {downloading[tipo] === 'downloading' ? 'Descargando...' : 'Descargar'}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>

          {isGestor && (
            <div className="plg-footer">
              <button
                type="button"
                className="plg-btn-outline"
                onClick={() => setShowRejectModal(true)}
                disabled={loadingAction}
              >
                <XCircle size={12} />
                Devolver
              </button>
              <button
                type="button"
                className="plg-btn-primary"
                onClick={handleApprove}
                disabled={loadingAction}
              >
                {loadingAction ? (
                  <LoaderCircle size={12} className="animate-spin" />
                ) : (
                  <CheckCircle size={12} />
                )}
                Aprobar
              </button>
            </div>
          )}
        </div>
      </div>

      {showRejectModal && (
        <div className="plg-modal-overlay">
          <div className="plg-modal">
            <div className="plg-modal__header">
              <h3 className="plg-modal__title">Devolver Documentos</h3>
              <button
                type="button"
                className="plg-modal__close"
                onClick={() => { setShowRejectModal(false); setRejectObservaciones(''); }}
                disabled={loadingAction}
              >
                <X size={16} />
              </button>
            </div>
            <p className="plg-modal__hint">
              Ingrese las observaciones para que el Director subsane los documentos.
            </p>
            <textarea
              className="plg-modal__textarea"
              rows={4}
              placeholder="Describa las observaciones..."
              value={rejectObservaciones}
              onChange={(e) => setRejectObservaciones(e.target.value)}
            />
            <div className="plg-modal__footer">
              <button
                type="button"
                className="plg-btn-outline"
                onClick={() => { setShowRejectModal(false); setRejectObservaciones(''); }}
                disabled={loadingAction}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="plg-btn-primary plg-btn-danger"
                onClick={handleReject}
                disabled={loadingAction || !rejectObservaciones.trim()}
              >
                {loadingAction ? <LoaderCircle size={12} className="animate-spin" /> : <XCircle size={12} />}
                Devolver
              </button>
            </div>
          </div>
        </div>
      )}

      {previewUrl && (
        <div className="plg-preview-overlay" onClick={handleClosePreview}>
          <div className="plg-preview-modal" onClick={(e) => e.stopPropagation()}>
            <div className="plg-preview-modal__header">
              <span className="plg-preview-modal__name">{previewName}</span>
              <button className="plg-preview-modal__close" onClick={handleClosePreview}>
                <X size={18} />
              </button>
            </div>
            <iframe
              src={previewUrl}
              className="plg-preview-modal__body"
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
