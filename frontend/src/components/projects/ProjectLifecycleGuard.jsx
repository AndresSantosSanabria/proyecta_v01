import { useEffect, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation, useNavigate, useParams } from 'react-router-dom';
import { LoaderCircle, Upload, FileCheck, AlertTriangle, CheckCircle, XCircle, Clock, Eye, Download, AlertOctagon, X, ShieldAlert } from 'lucide-react';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import { useAuthContext } from '../../context/AuthContext';
import ProjectOnboardingWizard from './ProjectOnboardingWizard';
import { emitToast } from '../../utils/feedback';
import { NO_ACCESS_MESSAGE, isForbiddenError } from '../../utils/accessMessages';
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
  const navigate = useNavigate();

  const DOC_LABELS = {
    VIABILIZACION: 'Documento de Viabilidad',
    PLAN_COMUNICACIONES: 'Plan de Comunicaciones',
    MATRIZ_RIESGOS_VIABILIDAD: 'Matriz de Riesgos de Viabilidad',
  };

  const docRevisions = useMemo(() => {
    const map = {};
    (completionStatus?.documentosPreWizard || []).forEach((r) => {
      map[r.tipoDocumento] = r;
    });
    return map;
  }, [completionStatus]);

  const uploadableTypes = Object.keys(DOC_LABELS).filter((tipo) => {
    if (!isResubmission) return true;
    const rev = docRevisions[tipo];
    if (rev?.estado === 'APROBADO') return false;
    if (rev?.estado === 'PENDIENTE') return false;
    return true;
  });

  const returnedTypes = Object.keys(DOC_LABELS).filter(
    (tipo) => docRevisions[tipo]?.estado === 'DEVUELTO'
  );

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

  const allFilesSelected = uploadableTypes.every((f) => files[f] !== null);

  const handleUploadAll = async () => {
    if (!allFilesSelected || !project?.id) return;
    setUploading(true);
    setUploadProgress({});

    try {
      for (const tipo of uploadableTypes) {
        const file = files[tipo];
        if (!file) continue;
        setUploadProgress((prev) => ({ ...prev, [tipo]: 'uploading' }));
        const observacion = isResubmission
          ? `Documento subsanado por el Director`
          : `Documento cargado durante el proceso de completar informacion del proyecto`;
        await documentService.cargarDocumento(project.id, tipo, file, observacion);
        setUploadProgress((prev) => ({ ...prev, [tipo]: 'done' }));
      }

      const count = uploadableTypes.length;
      emitToast({
        tone: 'success',
        title: isResubmission ? 'Documentos subsanados' : 'Documentos cargados',
        message: isResubmission
          ? `${count} documento(s) fueron cargados exitosamente. El Gestor sera notificado para su revision.`
          : `${count} documento(s) fueron cargados exitosamente. El Gestor sera notificado para su verificacion.`,
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
            <button
              type="button"
              className="plg-hero__close"
              onClick={() => navigate('/projects')}
              aria-label="Cerrar"
              title="Cerrar"
            >
              <X size={18} />
            </button>
            <span className={`plg-hero__badge ${isDevuelta ? 'plg-hero__badge--danger' : 'plg-hero__badge--warning'}`}>
              {isDevuelta ? 'Documentos Devueltos' : 'Paso 1 - Director de Proyecto'}
            </span>
            <h2 className="plg-hero__title">
              {isDevuelta ? 'Subsanar Documentos' : 'Cargar Documentos Pre-Wizard'}
            </h2>
            <p className="plg-hero__subtitle">
              {isDevuelta
                ? `El Gestor devolvio ${returnedTypes.length} documento(s) con observaciones. Subsanen solo los documentos devueltos y vuelva a cargarlos.`
                : 'Para continuar con la completitud del proyecto, primero debe cargar los 3 documentos requeridos. El Gestor verificara los documentos antes de que pueda acceder al wizard de completitud.'}
            </p>
          </div>

          {!isDevuelta && (
            <div className="project-onboarding__locked info-banner">
              <AlertTriangle size={16} />
              <div>
                <strong>Requisito previo</strong>
                <p>El boton "Completar Proyecto" se habilitara solo despues de que el Gestor verifique los 3 documentos.</p>
              </div>
            </div>
          )}

          <div className="plg-info-text">
            <h3 className="plg-info-text__title">DOCUMENTACIÓN CLAVE DEL PROYECTO</h3>
            <p className="plg-info-text__intro">
              Para garantizar una gestión sólida y una ejecución controlada, es indispensable cargar en el aplicativo tres documentos fundamentales del proyecto:
            </p>
            <ul className="plg-info-text__list">
              <li>
                <strong>Documentos de Viabilidad:</strong> Son la brújula que confirma que el proyecto es técnica, económica y legalmente posible. Sin ellos, no hay certeza de que el esfuerzo tenga un retorno o un impacto real.
              </li>
              <li>
                <strong>Plan de Comunicaciones:</strong> Define cómo, cuándo y a quién se informará. Una comunicación clara evita malentendidos, alinea a los interesados y mantiene a todo el equipo trabajando en la misma dirección.
              </li>
              <li>
                <strong>Matriz de Riesgos:</strong> Identifica las amenazas potenciales y las estrategias para mitigarlas. Anticiparse a los problemas es la mejor herramienta para proteger los plazos y el presupuesto.
              </li>
            </ul>
            <p className="plg-info-text__closing">
              Por favor, adjunte estos archivos a continuación para habilitar la siguiente fase del proyecto.
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
              {isResubmission
                ? uploadableTypes.length > 0
                  ? `Cargue los ${uploadableTypes.length} documento(s) devuelto(s). Los demas documentos no requieren accion.`
                  : 'Todos los documentos devueltos ya fueron cargados. El Gestor los revisara.'
                : 'Cargue los 3 documentos en formato PDF. Todos son obligatorios para continuar.'}
            </p>

            <div className="plg-doc-list">
              {Object.entries(DOC_LABELS).map(([tipo, label]) => {
                const revision = docRevisions[tipo];
                const isApproved = revision?.estado === 'APROBADO';
                const isReturned = revision?.estado === 'DEVUELTO';
                const file = files[tipo];
                const progress = uploadProgress[tipo];

                if (isApproved) {
                  return (
                    <div key={tipo} className="plg-doc-card plg-doc-card--has-file plg-doc-card--approved">
                      <div className="plg-doc-card__inner">
                        <div className="plg-doc-card__icon plg-doc-card__icon--approved">
                          <CheckCircle size={16} />
                        </div>
                        <div className="plg-doc-card__info">
                          <span className="plg-doc-card__name">{label}</span>
                          <span className="plg-doc-card__size">Verificado por el Gestor</span>
                        </div>
                        <span className="plg-doc-status plg-doc-status--approved">Verificado</span>
                      </div>
                    </div>
                  );
                }

                if (isResubmission && revision && !isReturned) {
                  return (
                    <div key={tipo} className="plg-doc-card plg-doc-card--has-file">
                      <div className="plg-doc-card__inner">
                        <div className="plg-doc-card__icon">
                          <FileCheck size={16} strokeWidth={2.2} />
                        </div>
                        <div className="plg-doc-card__info">
                          <span className="plg-doc-card__name">{label}</span>
                          <span className="plg-doc-card__size">Documento cargado - pendiente de revision</span>
                        </div>
                        <span className="plg-doc-status plg-doc-status--pending">En revision</span>
                      </div>
                    </div>
                  );
                }

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
                        <span className={`plg-doc-status ${isReturned ? 'plg-doc-status--returned' : 'plg-doc-status--pending'}`}>
                          {isReturned ? 'Devuelto' : 'Pendiente'}
                        </span>
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
                        {isReturned && (
                          <span className="plg-doc-status plg-doc-status--returned">Devuelto</span>
                        )}
                      </div>
                    )}
                    {isReturned && revision?.observacion && (
                      <div className="plg-doc-card__observation">
                        <AlertTriangle size={13} />
                        <p>{revision.observacion}</p>
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
                  {isResubmission
                    ? `Subsanar ${uploadableTypes.length} Documento(s)`
                    : 'Cargar Documentos'}
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
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectObservaciones, setRejectObservaciones] = useState('');
  const [loadingAction, setLoadingAction] = useState(false);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewName, setPreviewName] = useState('');
  const [downloading, setDownloading] = useState({});
  const [revisions, setRevisions] = useState([]);
  const navigate = useNavigate();

  const { hasRole, isAdminLocal, transversal } = useAuthContext();
  const isGestor = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_PROYECTOS') || hasRole('GESTOR_TIC');

  const DOC_LABELS = {
    VIABILIZACION: 'Documento de Viabilidad',
    PLAN_COMUNICACIONES: 'Plan de Comunicaciones',
    MATRIZ_RIESGOS_VIABILIDAD: 'Matriz de Riesgos de Viabilidad',
  };

  const loadRevisions = async () => {
    if (!project?.id) return;
    try {
      const data = await documentService.listarRevisionesPreWizard(project.id);
      setRevisions(data?.data?.documentos || data?.documentos || []);
    } catch {
      setRevisions([]);
    }
  };

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      if (!project?.id) return;
      try {
        const data = await documentService.listarRevisionesPreWizard(project.id);
        if (!cancelled) {
          setRevisions(data?.data?.documentos || data?.documentos || []);
        }
      } catch {
        if (!cancelled) setRevisions([]);
      }
    };
    void load();
    return () => { cancelled = true; };
  }, [project?.id]);

  const revisionMap = useMemo(() => {
    const map = {};
    revisions.forEach((r) => { map[r.tipoDocumento] = r; });
    return map;
  }, [revisions]);

  const aprobados = revisions.filter((r) => r.estado === 'APROBADO').length;
  const devueltos = revisions.filter((r) => r.estado === 'DEVUELTO').length;
  const pendientes = revisions.filter((r) => r.estado !== 'APROBADO' && r.estado !== 'DEVUELTO').length;
  const revisionCompleta = revisions.length >= 3 && pendientes === 0;
  const hayDevuelto = devueltos > 0;

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

  const handleApproveDoc = async (tipo) => {
    setLoadingAction(true);
    try {
      await documentService.aprobarDocumentoPreWizard(project.id, tipo);
      emitToast({
        tone: 'success',
        title: 'Documento verificado',
        message: `${DOC_LABELS[tipo]} fue verificado.`,
      });
      await loadRevisions();
      onRefresh?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error verificando documento';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setLoadingAction(false);
    }
  };

  const openRejectModal = (tipo) => {
    setRejectTarget(tipo);
    setRejectObservaciones('');
    setShowRejectModal(true);
  };

  const closeRejectModal = () => {
    setShowRejectModal(false);
    setRejectTarget(null);
    setRejectObservaciones('');
  };

  const handleRejectDoc = async () => {
    if (!rejectTarget) return;
    if (!rejectObservaciones.trim()) {
      emitToast({ tone: 'warning', title: 'Observaciones requeridas', message: 'Debe ingresar observaciones para devolver el documento.' });
      return;
    }
    setLoadingAction(true);
    try {
      await documentService.devolverDocumentoPreWizard(project.id, rejectTarget, rejectObservaciones.trim());
      emitToast({
        tone: 'success',
        title: 'Documento devuelto',
        message: `${DOC_LABELS[rejectTarget]} fue devuelto al Director con observaciones.`,
      });
      closeRejectModal();
      await loadRevisions();
      onRefresh?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error devolviendo documento';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setLoadingAction(false);
    }
  };

  const handleConfirmarRevision = async () => {
    setLoadingAction(true);
    try {
      const data = await documentService.confirmarRevisionPreWizard(project.id);
      const payload = data?.data || data;
      emitToast({
        tone: 'success',
        title: hayDevuelto ? 'Observaciones enviadas' : 'Verificacion confirmada',
        message: payload?.mensaje || 'Notificacion consolidada enviada al Director y Gestor.',
      });
      onRefresh?.();
      navigate('/projects');
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error confirmando la revision';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setLoadingAction(false);
    }
  };

  const statusInfo = (estado) => {
    if (estado === 'APROBADO') return { label: 'Verificado', className: 'plg-doc-status--approved', icon: <CheckCircle size={16} />, iconClass: 'plg-doc-card__icon--approved' };
    if (estado === 'DEVUELTO') return { label: 'Devuelto', className: 'plg-doc-status--returned', icon: <XCircle size={16} />, iconClass: 'plg-doc-card__icon--returned' };
    return { label: 'Pendiente', className: 'plg-doc-status--pending', icon: <Clock size={16} />, iconClass: '' };
  };

  return (
    <div className="project-onboarding">
      <div className="project-onboarding__shell">
        <div className="project-onboarding__panel card-surface">
          <div className="plg-hero">
            <button
              type="button"
              className="plg-hero__close"
              onClick={() => navigate('/projects')}
              aria-label="Cerrar"
              title="Cerrar"
            >
              <X size={18} />
            </button>
            <span className="plg-hero__badge plg-hero__badge--warning">
              Verificacion Individual - {aprobados}/3
            </span>
            <h2 className="plg-hero__title">Documentos Cargados</h2>
            <p className="plg-hero__subtitle">
              {isGestor
                ? 'Verifique o devuelva cada documento. Al terminar, use el boton final para enviar UNA sola notificacion consolidada.'
                : 'Los documentos estan en revision individual por parte del Gestor. El wizard se habilita cuando los 3 esten verificados.'}
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
              <strong className="plg-info-card__value plg-info-card__value--warning">{aprobados}/3 verificados - En revision</strong>
            </article>
          </section>

          <section className="plg-docs-section">
            <div className="plg-docs-section__header">
              <h3 className="plg-docs-section__title">Documentos Cargados</h3>
              <span className="plg-docs-section__count">3</span>
            </div>

            <div className="plg-doc-list">
              {Object.entries(DOC_LABELS).map(([tipo, label]) => {
                const revision = revisionMap[tipo];
                const estado = revision?.estado || 'PENDIENTE';
                const status = statusInfo(estado);
                const canDecide = isGestor && estado !== 'APROBADO';

                return (
                  <div
                    key={tipo}
                    className={`plg-doc-card plg-doc-card--has-file ${estado === 'APROBADO' ? 'plg-doc-card--approved' : ''} ${estado === 'DEVUELTO' ? 'plg-doc-card--returned' : ''}`}
                  >
                    <div className="plg-doc-card__inner">
                      <div className={`plg-doc-card__icon ${status.iconClass}`}>
                        {status.icon}
                      </div>
                      <div className="plg-doc-card__info">
                        <span className="plg-doc-card__name">{label}</span>
                        <span className="plg-doc-card__size">
                          {estado === 'APROBADO'
                            ? `Verificado${revision?.revisadoPor ? ` por ${revision.revisadoPor}` : ''}`
                            : estado === 'DEVUELTO'
                              ? 'Devuelto al Director'
                              : 'Pendiente de revision'}
                        </span>
                      </div>
                      <span className={`plg-doc-status ${status.className}`}>{status.label}</span>
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
                        {canDecide && (
                          <>
                            <button
                              type="button"
                              className="plg-doc-card__btn plg-doc-card__btn--approve"
                              onClick={() => handleApproveDoc(tipo)}
                              disabled={loadingAction}
                              title="Verificar documento"
                            >
                              <CheckCircle size={12} />
                              Verificar
                            </button>
                            <button
                              type="button"
                              className="plg-doc-card__btn plg-doc-card__btn--return"
                              onClick={() => openRejectModal(tipo)}
                              disabled={loadingAction}
                              title="Devolver documento"
                            >
                              <XCircle size={12} />
                              Devolver
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                    {estado === 'DEVUELTO' && revision?.observacion && (
                      <div className="plg-doc-card__observation">
                        <AlertTriangle size={13} />
                        <p>{revision.observacion}</p>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </section>

          {isGestor && revisionCompleta && (
            <section className="plg-confirm-bar" aria-label="Confirmar revision">
              <div className="plg-confirm-bar__info">
                <span className="plg-confirm-bar__count">
                  {aprobados}/3 verificados{devueltos > 0 ? ` · ${devueltos} devuelto(s)` : ''}
                </span>
                <p className="plg-confirm-bar__hint">
                  {hayDevuelto
                    ? 'Al finalizar se enviara UNA sola notificacion consolidada con el estado de los 3 documentos y las observaciones.'
                    : 'Al confirmar se enviara UNA sola notificacion consolidada de verificacion al Director y Gestor.'}
                </p>
              </div>
              <button
                type="button"
                className={`plg-btn-primary ${hayDevuelto ? 'plg-btn-danger' : ''}`}
                onClick={handleConfirmarRevision}
                disabled={loadingAction}
              >
                {loadingAction
                  ? <LoaderCircle size={12} className="animate-spin" />
                  : hayDevuelto ? <XCircle size={12} /> : <CheckCircle size={12} />}
                {loadingAction
                  ? 'Enviando...'
                  : hayDevuelto ? 'Finalizar observaciones' : 'Confirmar verificacion'}
              </button>
            </section>
          )}
          {isGestor && !revisionCompleta && (
            <section className="plg-confirm-bar plg-confirm-bar--muted" aria-label="Revision incompleta">
              <div className="plg-confirm-bar__info">
                <p className="plg-confirm-bar__hint">
                  Decida Verificar o Devolver los 3 documentos para habilitar el envio de la notificacion consolidada.
                </p>
              </div>
            </section>
          )}
        </div>
      </div>

      {showRejectModal && rejectTarget && (
        <div className="plg-modal-overlay">
          <div className="plg-modal">
            <div className="plg-modal__header">
              <h3 className="plg-modal__title">Devolver Documento</h3>
              <button
                type="button"
                className="plg-modal__close"
                onClick={closeRejectModal}
                disabled={loadingAction}
              >
                <X size={16} />
              </button>
            </div>
            <p className="plg-modal__hint">
              Documento: <strong>{DOC_LABELS[rejectTarget]}</strong>. Ingrese las observaciones para que el Director subsane este documento.
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
                onClick={closeRejectModal}
                disabled={loadingAction}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="plg-btn-primary plg-btn-danger"
                onClick={handleRejectDoc}
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
  const [showForceCloseModal, setShowForceCloseModal] = useState(false);
  const [forceCloseStep, setForceCloseStep] = useState('warning');
  const [forceCloseComment, setForceCloseComment] = useState('');
  const { hasRole, isAdminLocal, transversal } = useAuthContext();
  const isGestor = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_PROYECTOS') || hasRole('GESTOR_TIC');

  const openForceCloseModal = () => {
    setForceCloseComment('');
    setForceCloseStep('warning');
    setShowForceCloseModal(true);
  };

  const closeForceCloseModal = () => {
    if (loadingAction) return;
    setShowForceCloseModal(false);
    setForceCloseStep('warning');
    setForceCloseComment('');
  };

  const acceptForceCloseWarning = () => {
    setForceCloseStep('comment');
  };

  const backToWarningStep = () => {
    if (loadingAction) return;
    setForceCloseStep('warning');
  };

  const handleForceClose = async () => {
    const comment = forceCloseComment.trim();
    if (comment.length < 10) {
      emitToast({
        tone: 'warning',
        title: 'Comentario requerido',
        message: 'Debe ingresar un comentario de al menos 10 caracteres para continuar.',
      });
      return;
    }
    setLoadingAction(true);
    try {
      await projectService.cerrarForzoso(project.id, comment);
      emitToast({
        tone: 'success',
        title: 'Proyecto cerrado',
        message: 'El proyecto fue cerrado forzosamente (cierre extraordinario).',
      });
      setShowForceCloseModal(false);
      setForceCloseComment('');
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
                onClick={openForceCloseModal}
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

      {showForceCloseModal && forceCloseStep === 'warning' && (
        <div className="plg-modal-overlay" onClick={closeForceCloseModal}>
          <div
            className="plg-modal plg-modal--danger"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="plg-force-close-warning-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="plg-modal__header">
              <div className="plg-modal__header-title">
                <span className="plg-modal__icon" aria-hidden="true">
                  <ShieldAlert size={16} />
                </span>
                <h3 className="plg-modal__title" id="plg-force-close-warning-title">Advertencia: cierre forzoso</h3>
              </div>
              <button
                type="button"
                className="plg-modal__close"
                onClick={closeForceCloseModal}
                aria-label="Cerrar"
              >
                <X size={16} />
              </button>
            </div>

            <div className="plg-danger-alert" role="alert">
              <div className="plg-danger-alert__icon" aria-hidden="true">
                <AlertTriangle size={16} />
              </div>
              <div className="plg-danger-alert__body">
                <strong>¿Está seguro de cerrar el proyecto?</strong>
                <p>
                  Esta acción <mark>NO es reversible</mark>. El proyecto pasará al estado{' '}
                  <strong>CERRADO FORZOSO</strong> y no podrá reabrirse por el flujo normal.
                </p>
              </div>
            </div>

            <p className="plg-modal__hint">
              Proyecto: <strong>{project?.nombre || project?.id}</strong>.
              Si confirma, deberá indicar el motivo del cierre en el siguiente paso.
            </p>

            <div className="plg-modal__footer">
              <button
                type="button"
                className="plg-btn-outline"
                onClick={closeForceCloseModal}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="plg-btn-primary plg-btn-danger"
                onClick={acceptForceCloseWarning}
              >
                <AlertOctagon size={12} />
                Sí, continuar
              </button>
            </div>
          </div>
        </div>
      )}

      {showForceCloseModal && forceCloseStep === 'comment' && (
        <div className="plg-modal-overlay" onClick={closeForceCloseModal}>
          <div
            className="plg-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="plg-force-close-comment-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="plg-modal__header">
              <div className="plg-modal__header-title">
                <span className="plg-modal__icon" aria-hidden="true">
                  <ShieldAlert size={16} />
                </span>
                <h3 className="plg-modal__title" id="plg-force-close-comment-title">Cierre forzoso / extraordinario</h3>
              </div>
              <button
                type="button"
                className="plg-modal__close"
                onClick={closeForceCloseModal}
                disabled={loadingAction}
                aria-label="Cerrar"
              >
                <X size={16} />
              </button>
            </div>

            <p className="plg-modal__hint">
              Proyecto: <strong>{project?.nombre || project?.id}</strong>.
              Para confirmar, escriba el motivo del cierre (mínimo 10 caracteres).
            </p>
            <textarea
              className="plg-modal__textarea"
              rows={4}
              placeholder="Motivo del cierre extraordinario (minimo 10 caracteres)..."
              value={forceCloseComment}
              onChange={(e) => setForceCloseComment(e.target.value)}
              disabled={loadingAction}
              autoFocus
            />
            <div className="plg-modal__footer">
              <button
                type="button"
                className="plg-btn-outline"
                onClick={backToWarningStep}
                disabled={loadingAction}
              >
                Volver
              </button>
              <button
                type="button"
                className="plg-btn-primary plg-btn-danger"
                onClick={handleForceClose}
                disabled={loadingAction || forceCloseComment.trim().length < 10}
              >
                {loadingAction ? <LoaderCircle size={12} className="animate-spin" /> : <AlertOctagon size={12} />}
                {loadingAction ? 'Cerrando...' : 'Sí, cerrar definitivamente'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const ProjectLifecycleGuard = () => {
  const { id, codigoProyecto } = useParams();
  const location = useLocation();
  const { isVisualizador, hasRole, isAdminLocal, transversal } = useAuthContext();
  const isGestor = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_PROYECTOS') || hasRole('GESTOR_TIC');
  const projectId = useMemo(() => String(id || codigoProyecto || '').trim(), [codigoProyecto, id]);
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState(null);
  const [completionStatus, setCompletionStatus] = useState(null);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const refreshLifecycle = async (silent = false) => {
    if (!projectId) return;
    if (isVisualizador) {
      setError(NO_ACCESS_MESSAGE);
      setLoading(false);
      return;
    }
    try {
      if (!silent) setLoading(true);
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
    } catch (fetchError) {
      console.error('No fue posible cargar el ciclo de vida del proyecto:', fetchError);
      if (isVisualizador || isForbiddenError(fetchError)) {
        setError(NO_ACCESS_MESSAGE);
      } else {
        setError('No fue posible cargar el proyecto.');
      }
    } finally {
      if (!silent) setLoading(false);
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [projectId, isVisualizador]);

  const silentRefresh = async () => {
    await refreshLifecycle(true);
  };

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
    if (isGestor) {
      return (
        <DocumentosCargadosView
          project={project}
          onRefresh={silentRefresh}
        />
      );
    }
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
        onRefresh={silentRefresh}
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
      />
    );
  }

  return <Outlet />;
};

export default ProjectLifecycleGuard;
