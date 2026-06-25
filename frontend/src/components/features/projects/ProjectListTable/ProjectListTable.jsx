import { useNavigate } from 'react-router-dom';
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

const ProjectListTable = ({ projects = [], loading = false, canEditProject = false }) => {
  const navigate = useNavigate();

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
                const currentStatus = statusMap[project.estado] || {
                  label: project.estado,
                  class: 'default',
                  tooltip: `Estado registrado: ${project.estado}`,
                };

                return (
                  <tr key={projectId || index}>
                    <td data-label="Acciones">
                      <div className="action-buttons">
                        <button
                          className="btn-action-open"
                          onClick={() => navigate(`/projects/${projectId}/progress`)}
                          disabled={!projectId}
                        >
                          Abrir
                        </button>
                        {canEditProject ? <button className="btn-action-edit">Editar</button> : null}
                      </div>
                    </td>
                    <td className="col-code" data-label="Código">{projectId || 'Sin codigo'}</td>
                    <td className="col-name" data-label="Nombre">{getProjectName(project)}</td>
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
                        title={currentStatus.tooltip}
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
            const currentStatus = statusMap[project.estado] || {
              label: project.estado,
              class: 'default',
              tooltip: `Estado registrado: ${project.estado}`,
            };

            return (
              <article key={projectId || index} className="project-list-card card-surface">
                <div className="project-list-card__top">
                  <div className="project-list-card__code">{projectId || 'Sin codigo'}</div>
                  <span
                    className={`status-badge status-badge--tooltip ${currentStatus.class}`}
                    data-tooltip={currentStatus.tooltip}
                    aria-label={`${currentStatus.label}. ${currentStatus.tooltip}`}
                    title={currentStatus.tooltip}
                  >
                    {currentStatus.label}
                  </span>
                </div>

                <div className="project-list-card__title">
                  <strong>{getProjectName(project)}</strong>
                  <span>{getProjectDependency(project)}</span>
                </div>

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
                    className="btn-action-open"
                    onClick={() => navigate(`/projects/${projectId}/progress`)}
                    disabled={!projectId}
                  >
                    Abrir proyecto
                  </button>
                  {canEditProject ? <button className="btn-action-edit">Editar</button> : null}
                </div>
              </article>
            );
          })
        ) : (
          <div className="empty-row">No se encontraron proyectos</div>
        )}
      </div>
    </div>
  );
};

export default ProjectListTable;
