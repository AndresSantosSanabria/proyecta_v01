import React from 'react';
import { useNavigate } from 'react-router-dom';
import './ProjectTable.css';
import { Plus, ChevronRight } from 'lucide-react';

const ProjectTable = ({ projects = [], loading = false }) => {
  const navigate = useNavigate();

  if (loading) {
    return <div className="table-loading">Cargando proyectos...</div>;
  }

  return (
    <div className="project-table-container">
      <div className="table-header-row">
        <h2>Resumen de Proyectos</h2>
        <button className="btn-new-project" onClick={() => navigate('/proyectos/nuevo')}>
          <Plus size={18} />
          <span>Nuevo Proyecto</span>
        </button>
      </div>
      
      <div className="table-responsive">
        <table className="project-table">
          <thead>
            <tr>
              <th>CÓDIGO</th>
              <th>NOMBRE</th>
              <th>DEPENDENCIA</th>
              <th>AVANCE</th>
              <th>ESTADO</th>
              <th>ENTREGABLES</th>
              <th>ACCIÓN</th>
            </tr>
          </thead>
          <tbody>
            {projects.length > 0 ? (
              projects.map((project, index) => {
                const statusMap = {
                  'CON_RETRASOS': { label: 'Con retrasos', class: 'warning' },
                  'ACTIVO': { label: 'Activo', class: 'success' },
                  'CERRADO': { label: 'Cerrado', class: 'default' }
                };
                
                const currentStatus = statusMap[project.estado] || { label: project.estado, class: 'default' };

                return (
                  <tr key={project.id || index}>
                    <td className="col-code">{project.codigo}</td>
                    <td className="col-name">{project.nombreProyecto}</td>
                    <td className="col-dept">{project.nombreDependencia}</td>
                    <td className="col-progress">
                      <div className="progress-cell">
                        <div className="progress-bar-bg">
                          <div 
                            className={`progress-bar-fill ${project.avance === 0 ? 'low' : ''}`} 
                            style={{ width: `${project.avance}%` }}
                          ></div>
                        </div>
                        <span className="progress-text">{Math.round(project.avance)}%</span>
                      </div>
                    </td>
                    <td>
                      <span className={`status-badge ${currentStatus.class}`}>
                        {currentStatus.label}
                      </span>
                    </td>
                    <td>
                      <span className={project.entregablesAtrasados > 0 ? 'deliverables-badge' : 'deliverables-ok'}>
                        {project.entregablesAtrasados} {project.entregablesAtrasados === 1 ? 'atrasado' : 'atrasados'}
                      </span>
                    </td>
                    <td>
                      <button
                        className="btn-view-project"
                        onClick={() => navigate(`/proyectos/${(project.codigo || project.id).toLowerCase()}/avance`)}
                      >
                        Ver proyecto
                      </button>
                    </td>
                  </tr>
                );
              })
            ) : (
              <tr>
                <td colSpan="7" className="empty-row">No se encontraron proyectos</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ProjectTable;
