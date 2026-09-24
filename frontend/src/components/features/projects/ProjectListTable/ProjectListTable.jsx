import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, ShieldAlert } from 'lucide-react';
import projectService from '../../../../services/projectService';
import { emitToast } from '../../../../utils/feedback';
import './ProjectListTable.css';

const statusMap = {
  PENDIENTE: {
    label: 'Pendiente',
    class: 'warning',
    tooltip: 'Proyecto creado, pero aún no ha sido completado por el director.',
  },
  PENDIENTE_COMPLETAR: {
    label: 'Pendiente completar',
    class: 'warning',
    tooltip: 'El proyecto requiere que el director complete la información obligatoria.',
  },
  CON_RETRASOS: {
    label: 'Con retrasos',
    class: 'warning',
    tooltip: 'El proyecto avanza, pero tiene entregables vencidos o fuera de plazo.',
  },
  ACTIVO: {
    label: 'Activo',
    class: 'success',
    tooltip: 'El proyecto está en ejecución y dentro del flujo operativo normal.',
  },
  CERRADO: {
    label: 'Cerrado',
    class: 'default',
    tooltip: 'El proyecto finalizó y ya no recibe gestión operativa.',
  },
  CERRADO_FORZOSO: {
    label: 'Cerrado forzoso',
    class: 'danger',
    tooltip: 'El proyecto fue cerrado de forma extraordinaria sin cumplir el flujo normal de cierre.',
  },
  PLANIFICACION: {
    label: 'Planificación',
    class: 'default',
    tooltip: 'El proyecto está definido, pero todavía no entra en ejecución.',
  },
};

const getProjectId = (project) => project?.codigo || project?.id || project?.proyectoId || project?.proyecto_id || '';
const getProjectName = (project) => project?.nombre || project?.nombreProyecto || project?.name || 'Sin nombre';
const getProjectDependency = (project) => project?.dependencia || project?.nombreDependencia || 'Sin dependencia';
const getProjectDirectorName = (project) => project?.director || project?.directorNombre || '';
const getProjectDirectorCargo = (project) => project?.directorCargo || project?.cargoDirector || project?.directorRol || '';
const buildProjectProgressPath = (projectId) => `/projects/${encodeURIComponent(projectId)}/progress`;

const openProject = (navigate, projectId) => (event) => {
  event?.preventDefault?.();
  event?.stopPropagation?.();

  if (!projectId) {
    return;
  }

  navigate(buildProjectProgressPath(projectId));
};

const ProjectListTable = ({
  projects = [],
  loading = false,
  canEditProject = false,
  onEditProject = null,
  canForceClose = false,
  onForceClosed = null,
}) => {
  const navigate = useNavigate();
  const [forceCloseTarget, setForceCloseTarget] = useState(null);
  const [forceCloseStep, setForceCloseStep] = useState('warning');
  const [forceCloseComment, setForceCloseComment] = useState('');
  const [forceClosing, setForceClosing] = useState(false);

  const openForceClose = (project, event) => {
    event?.preventDefault?.();
    event?.stopPropagation?.();
    setForceCloseTarget(project);
    setForceCloseStep('warning');
    setForceCloseComment('');
  };

  const closeForceCloseModal = () => {
    if (forceClosing) return;
    setForceCloseTarget(null);
    setForceCloseStep('warning');
    setForceCloseComment('');
  };

  const acceptForceCloseWarning = () => {
    setForceCloseStep('comment');
  };

  const backToWarningStep = () => {
    if (forceClosing) return;
    setForceCloseStep('warning');
  };

  const confirmForceClose = async () => {
    const comment = forceCloseComment.trim();
    if (comment.length < 10) {
      emitToast({
        tone: 'warning',
        title: 'Comentario requerido',
        message: 'Debe ingresar un comentario de al menos 10 caracteres para continuar.',
      });
      return;
    }
    const projectId = getProjectId(forceCloseTarget);
    if (!projectId) return;
    setForceClosing(true);
    try {
      await projectService.cerrarForzoso(projectId, comment);
      emitToast({
        tone: 'success',
        title: 'Proyecto cerrado',
        message: 'El proyecto fue cerrado forzosamente (cierre extraordinario).',
      });
      setForceCloseTarget(null);
      setForceCloseComment('');
      onForceClosed?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.message || 'Error cerrando el proyecto';
      emitToast({ tone: 'error', title: 'Error', message: detail });
    } finally {
      setForceClosing(false);
    }
  };

  const isTerminalStatus = (project) => {
    const estado = String(project?.estado || '').toUpperCase();
    return estado === 'CERRADO' || estado === 'CERRADO_FORZOSO' || estado === 'FINALIZADO';
  };

  if (loading) {
    return <div className="table-loading">Cargando proyectos...</div>;
  }

  return (
    <div className="project-list-table-container">
      <div className="table-responsive desktop-only">
        <table className="project-list-table">
          <thead>
            <tr>
              <th>ACCIONES</th>
              <th>CÓDIGO</th>
              <th>NOMBRE DEL PROYECTO</th>
              <th>DEPENDENCIA</th>
              <th>DIRECTOR</th>
              <th>PETI</th>
              <th>AVANCE</th>
              <th>ESTADO</th>
            </tr>
          </thead>
          <tbody>
            {projects.length > 0 ? (
              projects.map((project, index) => {
                const projectId = getProjectId(project);
                const baseStatus = statusMap[project.estado] || {
                  label: project.estado,
                  class: 'default',
                  tooltip: `Estado registrado: ${project.estado}`,
                };

                let currentStatus = baseStatus;
                if (project.estado === 'PENDIENTE_COMPLETAR') {
                  const viab = (project.viabilidadEstado || '').toUpperCase();
                  if (viab === 'CARGADA') {
                    currentStatus = {
                      label: 'Docs. pendientes verificación',
                      class: 'info',
                      tooltip: 'El Director cargó los documentos de viabilidad. Pendiente que el Gestor o Administrador verifique y apruebe.',
                    };
                  } else if (viab === 'DEVUELTA') {
                    currentStatus = {
                      label: 'Docs. devueltos',
                      class: 'warning',
                      tooltip: 'El Gestor devolvió los documentos con observaciones. El Director debe subsanar.',
                    };
                  } else if (viab === 'APROBADA') {
                    currentStatus = {
                      label: 'Pendiente por completar',
                      class: 'warning',
                      tooltip: 'La viabilidad fue aprobada. El Director debe completar la información del proyecto.',
                    };
                  }
                }

                if (project.estado === 'CERRADO_FORZOSO') {
                  const motivo = (project.cierreObservaciones || '').trim();
                  const por = (project.cierreForzosoPor || '').trim();
                  const when = project.cierreForzosoEn
                    ? new Date(project.cierreForzosoEn).toLocaleString('es-CO')
                    : '';
                  const parts = [];
                  if (motivo) parts.push(`Motivo: ${motivo}`);
                  if (por) parts.push(`Cerrado por: ${por}`);
                  if (when) parts.push(`Fecha: ${when}`);
                  currentStatus = {
                    label: 'Cerrado forzoso',
                    class: 'danger',
                    tooltip: parts.length
                      ? parts.join(' | ')
                      : 'El proyecto fue cerrado de forma extraordinaria sin cumplir el flujo normal de cierre.',
                  };
                }

                return (
                  <tr key={projectId || index}>
                    <td data-label="Acciones">
                      <div className="action-buttons">
                        <button
                          type="button"
                          className="btn-action-open"
                          onClick={openProject(navigate, projectId)}
                          disabled={!projectId}
                        >
                          Abrir
                        </button>
                        {canEditProject && onEditProject ? (
                          <button
                            type="button"
                            className="btn-action-edit"
                            onClick={() => onEditProject(projectId)}
                          >
                            Editar
                          </button>
                        ) : null}
                        {canForceClose && projectId && !isTerminalStatus(project) ? (
                          <button
                            type="button"
                            className="btn-action-force-close"
                            onClick={(event) => openForceClose(project, event)}
                            title="Cierre forzoso / extraordinario con comentario"
                          >
                            Cerrar
                          </button>
                        ) : null}
                      </div>
                    </td>
                    <td className="col-code" data-label="Código">{projectId || 'Sin código'}</td>
                    <td className="col-name" data-label="Nombre">
                      <div className="project-name-cell">
                        <span>{getProjectName(project)}</span>
                        {project.estado === 'CERRADO_FORZOSO' && project.cierreObservaciones ? (
                          <span className="force-close-reason" title={project.cierreObservaciones}>
                            Motivo: {project.cierreObservaciones}
                          </span>
                        ) : null}
                      </div>
                    </td>
                    <td className="col-dept" data-label="Dependencia">{getProjectDependency(project)}</td>
                    <td className="col-director" data-label="Director">
                      <div className="director-cell">
                        <strong>{getProjectDirectorName(project)}</strong>
                        {getProjectDirectorCargo(project) ? (
                          <span>{getProjectDirectorCargo(project)}</span>
                        ) : null}
                      </div>
                    </td>
                    <td data-label="PETI">
                      {project.peti ? (
                        <span className="peti-badge">PETI</span>
                      ) : (
                        <span className="peti-badge no-peti">No PETI</span>
                      )}
                    </td>
                    <td className="col-progress" data-label="Avance">
                      <div className="progress-cell">
                        <div className="progress-bar-bg">
                          <div
                            className="progress-bar-fill"
                            style={{ width: `${project.avanceTotal || 0}%` }}
                          />
                        </div>
                        <span className="progress-text">{Math.round(project.avanceTotal || 0)}%</span>
                      </div>
                    </td>
                    <td data-label="Estado">
                      <span
                        className={`status-badge status-badge--tooltip ${currentStatus.class}`}
                        data-tooltip={currentStatus.tooltip}
                        aria-label={`${currentStatus.label}. ${currentStatus.tooltip}`}
                      >
                        {currentStatus.label}
                      </span>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="8" className="empty-row">
                  No se encontraron proyectos
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="project-list-cards mobile-only">
        {projects.length > 0 ? (
          projects.map((project, index) => {
            const projectId = getProjectId(project);
            let currentStatus = statusMap[project.estado] || {
              label: project.estado,
              class: 'default',
              tooltip: `Estado registrado: ${project.estado}`,
            };
            if (project.estado === 'CERRADO_FORZOSO') {
              const motivo = (project.cierreObservaciones || '').trim();
              const por = (project.cierreForzosoPor || '').trim();
              const when = project.cierreForzosoEn
                ? new Date(project.cierreForzosoEn).toLocaleString('es-CO')
                : '';
              const parts = [];
              if (motivo) parts.push(`Motivo: ${motivo}`);
              if (por) parts.push(`Cerrado por: ${por}`);
              if (when) parts.push(`Fecha: ${when}`);
              currentStatus = {
                label: 'Cerrado forzoso',
                class: 'danger',
                tooltip: parts.length
                  ? parts.join(' | ')
                  : 'El proyecto fue cerrado de forma extraordinaria sin cumplir el flujo normal de cierre.',
              };
            }

            return (
              <article key={projectId || index} className="project-list-card card-surface">
                <div className="project-list-card__top">
                  <div className="project-list-card__code">{projectId || 'Sin código'}</div>
                  <span
                    className={`status-badge status-badge--tooltip ${currentStatus.class}`}
                    data-tooltip={currentStatus.tooltip}
                    aria-label={`${currentStatus.label}. ${currentStatus.tooltip}`}
                  >
                    {currentStatus.label}
                  </span>
                </div>

                <div className="project-list-card__title">
                  <strong>{getProjectName(project)}</strong>
                  <span>{getProjectDependency(project)}</span>
                </div>

                {project.estado === 'CERRADO_FORZOSO' && project.cierreObservaciones ? (
                  <div className="force-close-reason force-close-reason--card" title={project.cierreObservaciones}>
                    Motivo: {project.cierreObservaciones}
                  </div>
                ) : null}

                <div className="project-list-card__meta">
                  <div>
                    <span>Director</span>
                    <strong>{getProjectDirectorName(project) || 'Sin director'}</strong>
                  </div>
                  <div>
                    <span>Avance</span>
                    <strong>{Math.round(project.avanceTotal || 0)}%</strong>
                  </div>
                  <div>
                    <span>PETI</span>
                    <strong>{project.peti ? 'Sí' : 'No'}</strong>
                  </div>
                </div>

                <div className="project-list-card__actions">
                  <button
                    type="button"
                    className="btn-action-open"
                    onClick={openProject(navigate, projectId)}
                    disabled={!projectId}
                  >
                    Abrir proyecto
                  </button>
                  {canEditProject && onEditProject ? (
                    <button
                      type="button"
                      className="btn-action-edit"
                      onClick={() => onEditProject(projectId)}
                    >
                      Editar
                    </button>
                  ) : null}
                  {canForceClose && projectId && !isTerminalStatus(project) ? (
                    <button
                      type="button"
                      className="btn-action-force-close"
                      onClick={(event) => openForceClose(project, event)}
                    >
                      Cerrar forzoso
                    </button>
                  ) : null}
                </div>
              </article>
            );
          })
        ) : (
          <div className="empty-row">No se encontraron proyectos</div>
        )}
      </div>

      {forceCloseTarget && forceCloseStep === 'warning' && (
        <div className="force-close-overlay" onClick={closeForceCloseModal}>
          <div
            className="force-close-modal force-close-modal--danger"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="force-close-warning-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="force-close-modal__header">
              <div className="force-close-modal__header-title">
                <span className="force-close-modal__icon" aria-hidden="true">
                  <ShieldAlert size={18} />
                </span>
                <h3 id="force-close-warning-title">Advertencia: cierre forzoso</h3>
              </div>
              <button
                type="button"
                className="force-close-modal__close"
                onClick={closeForceCloseModal}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>

            <div className="force-close-danger" role="alert">
              <div className="force-close-danger__icon" aria-hidden="true">
                <AlertTriangle size={18} />
              </div>
              <div className="force-close-danger__body">
                <strong>¿Está seguro de cerrar el proyecto?</strong>
                <p>
                  Esta acción <mark>NO es reversible</mark>. El proyecto pasará al estado{' '}
                  <strong>CERRADO FORZOSO</strong> y no podrá reabrirse por el flujo normal.
                </p>
              </div>
            </div>

            <p className="force-close-modal__hint">
              Proyecto: <strong>{getProjectName(forceCloseTarget)}</strong> ({getProjectId(forceCloseTarget)})
              <br />
              Si confirma, deberá indicar el motivo del cierre en el siguiente paso.
            </p>

            <div className="force-close-modal__footer">
              <button
                type="button"
                className="btn-action-edit"
                onClick={closeForceCloseModal}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="btn-action-force-close btn-action-force-close--confirm"
                onClick={acceptForceCloseWarning}
              >
                Sí, continuar
              </button>
            </div>
          </div>
        </div>
      )}

      {forceCloseTarget && forceCloseStep === 'comment' && (
        <div className="force-close-overlay" onClick={closeForceCloseModal}>
          <div
            className="force-close-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="force-close-comment-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="force-close-modal__header">
              <div className="force-close-modal__header-title">
                <span className="force-close-modal__icon" aria-hidden="true">
                  <ShieldAlert size={18} />
                </span>
                <h3 id="force-close-comment-title">Cierre forzoso / extraordinario</h3>
              </div>
              <button
                type="button"
                className="force-close-modal__close"
                onClick={closeForceCloseModal}
                disabled={forceClosing}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>

            <p className="force-close-modal__hint">
              Proyecto: <strong>{getProjectName(forceCloseTarget)}</strong> ({getProjectId(forceCloseTarget)})
              <br />
              Para confirmar, escriba el motivo del cierre (mínimo 10 caracteres).
            </p>
            <textarea
              className="force-close-modal__textarea"
              rows={4}
              placeholder="Motivo del cierre extraordinario (minimo 10 caracteres)..."
              value={forceCloseComment}
              onChange={(e) => setForceCloseComment(e.target.value)}
              disabled={forceClosing}
              autoFocus
            />
            <div className="force-close-modal__footer">
              <button
                type="button"
                className="btn-action-edit"
                onClick={backToWarningStep}
                disabled={forceClosing}
              >
                Volver
              </button>
              <button
                type="button"
                className="btn-action-force-close btn-action-force-close--confirm"
                onClick={confirmForceClose}
                disabled={forceClosing || forceCloseComment.trim().length < 10}
              >
                {forceClosing ? 'Cerrando...' : 'Sí, cerrar definitivamente'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProjectListTable;
