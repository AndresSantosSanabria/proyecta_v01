import { useNavigate } from 'react-router-dom';
import './ProjectListTable.css';

const statusMap = {
  CON_RETRASOS: { label: 'Con retrasos', class: 'warning' },
  ACTIVO: { label: 'Activo', class: 'success' },
  CERRADO: { label: 'Cerrado', class: 'default' },
};

const getProjectId = (project) => project?.codigo || project?.id || project?.proyectoId || project?.proyecto_id || '';
const getProjectName = (project) => project?.nombre || project?.nombreProyecto || project?.name || 'Sin nombre';
const getProjectDependency = (project) => project?.dependencia || project?.nombreDependencia || 'Sin dependencia';

const ProjectListTable = ({ projects = [], loading = false, canEditProject = false }) => {
  const navigate = useNavigate();

  if (loading) {
    return <div className="table-loading">Cargando proyectos...</div>;
  }

  return (
    <div className="project-list-table-container">
      <div className="table-responsive">
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
                const currentStatus = statusMap[project.estado] || { label: project.estado, class: 'default' };

                return (
                  <tr key={projectId || index}>
                    <td>
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
                    <td className="col-code">{projectId || 'Sin codigo'}</td>
                    <td className="col-name">{getProjectName(project)}</td>
                    <td className="col-dept">{getProjectDependency(project)}</td>
                    <td className="col-director">{project.director || 'No asignado'}</td>
                    <td>
                      {project.peti ? (
                        <span className="peti-badge">PETI</span>
                      ) : (
                        <span className="peti-badge no-peti">No PETI</span>
                      )}
                    </td>
                    <td className="col-progress">
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
                    <td>
                      <span className={`status-badge ${currentStatus.class}`}>{currentStatus.label}</span>
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
    </div>
  );
};

export default ProjectListTable;
