import { useNavigate } from 'react-router-dom';
import './ProjectListTable.css';

const statusMap = {
  CON_RETRASOS: { label: 'Con retrasos', class: 'warning' },
  ACTIVO: { label: 'Activo', class: 'success' },
  CERRADO: { label: 'Cerrado', class: 'default' },
};

const ProjectListTable = ({ projects = [], loading = false }) => {
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
                const currentStatus = statusMap[project.estado] || { label: project.estado, class: 'default' };

                return (
                  <tr key={project.id || index}>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="btn-action-open"
                          onClick={() => navigate(`/projects/${project.codigo || project.id}/progress`)}
                        >
                          Abrir
                        </button>
                        <button className="btn-action-edit">Editar</button>
                      </div>
                    </td>
                    <td className="col-code">{project.codigo}</td>
                    <td className="col-name">{project.nombre}</td>
                    <td className="col-dept">{project.dependencia}</td>
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
