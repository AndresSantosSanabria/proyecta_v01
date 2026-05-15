import React, { useState, useEffect } from 'react';
import ProjectListTable from '../components/projects/ProjectListTable/ProjectListTable';
import projectService from '../services/projectService';
import { Plus, Search, Filter } from 'lucide-react';
import './ProjectsPage.css';

const ProjectsPage = () => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchProjects = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await projectService.getAll();
        
        // Manejamos tanto si viene un array directo como si viene paginado (objeto content)
        const projectsData = response.data?.content || response.data;
        
        if (response.success && Array.isArray(projectsData)) {
          setProjects(projectsData);
        } else {
          setError(response.message || 'Error al cargar el listado de proyectos');
        }
      } catch (err) {
        console.error('Error fetching projects:', err);
        setError('No se pudo establecer conexión con el servidor');
      } finally {
        setLoading(false);
      }
    };

    fetchProjects();
  }, []);

  // Validación de seguridad: Nos aseguramos de que 'projects' sea un array antes de filtrar
  const projectList = Array.isArray(projects) ? projects : [];

  const filteredProjects = projectList.filter(project => {
    const matchesSearch = 
      (project.nombre?.toLowerCase() || '').includes(searchTerm.toLowerCase()) ||
      (project.codigo?.toLowerCase() || '').includes(searchTerm.toLowerCase());
    
    const matchesStatus = statusFilter === 'all' || project.estado === statusFilter;
    
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="projects-container">
      <header className="projects-header">
        <div className="header-title-group">
          <h1>Proyectos TIC</h1>
          <p className="subtitle">Gestión y seguimiento de todos los proyectos</p>
        </div>
        <button className="btn-new-project">
          <Plus size={18} />
          <span>Nuevo Proyecto</span>
        </button>
      </header>

      <section className="filters-section">
        <div className="search-box">
          <Search size={18} className="search-icon" />
          <input 
            type="text" 
            placeholder="Buscar por nombre o código..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
        
        <div className="select-filters">
          <select className="filter-select">
            <option value="all">Todos los proyectos</option>
          </select>
          
          <select 
            className="filter-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="all">Todos los estados</option>
            <option value="ACTIVO">Activo</option>
            <option value="CON_RETRASOS">Con retrasos</option>
            <option value="CERRADO">Cerrado</option>
          </select>
        </div>
      </section>

      {error && (
        <div className="error-banner">
          {error}
          <button onClick={() => window.location.reload()} className="btn-retry">Reintentar</button>
        </div>
      )}

      <ProjectListTable 
        projects={filteredProjects} 
        loading={loading} 
      />
    </div>
  );
};

export default ProjectsPage;
