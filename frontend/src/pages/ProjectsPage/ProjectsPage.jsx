import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Filter, Plus, RefreshCw, Search, X } from 'lucide-react';
import ProjectListTable from '../../components/features/projects/ProjectListTable';
import projectService from '../../services/projectService';
import { usePermission } from '../../hooks/usePermission';
import './ProjectsPage.css';

const DEFAULT_FILTERS = {
  query: '',
  dependency: 'all',
  status: 'all',
  peti: 'all',
};

const normalizeText = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();

const ProjectsPage = () => {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const canCreateProject = usePermission('PROYECTO:CREAR');
  const navigate = useNavigate();

  const fetchProjects = async () => {
    try {
      setLoading(true);
      setError(null);
      const projectsData = await projectService.getAllUnpaged();
      setProjects(Array.isArray(projectsData) ? projectsData : []);
    } catch (err) {
      console.error('Error fetching projects:', err);
      setError('No se pudo establecer conexion con el servidor');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const projectsData = await projectService.getAllUnpaged();
        setProjects(Array.isArray(projectsData) ? projectsData : []);
      } catch (err) {
        console.error('Error fetching projects:', err);
        setError('No se pudo establecer conexion con el servidor');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const projectList = useMemo(() => (Array.isArray(projects) ? projects : []), [projects]);

  const dependencyOptions = useMemo(() => {
    return [...new Set(projectList.map((project) => project.dependencia).filter(Boolean))]
      .sort((left, right) => left.localeCompare(right, 'es'))
      .map((value) => ({ value, label: value }));
  }, [projectList]);

  const filteredProjects = useMemo(() => {
    const query = normalizeText(filters.query);
    return projectList.filter((project) => {
      const haystack = [
        project.codigo,
        project.nombre,
        project.nombreProyecto,
        project.dependencia,
        project.nombreDependencia,
        project.director,
        project.estado,
        project.peti ? 'peti' : 'no peti',
      ]
        .filter(Boolean)
        .join(' ');

      const matchesSearch = !query || normalizeText(haystack).includes(query);
      const matchesStatus = filters.status === 'all' || normalizeText(project.estado) === normalizeText(filters.status);
      const matchesDependency =
        filters.dependency === 'all' ||
        normalizeText(project.dependencia) === normalizeText(filters.dependency) ||
        normalizeText(project.nombreDependencia) === normalizeText(filters.dependency);
      const matchesPeti =
        filters.peti === 'all' ||
        (filters.peti === 'peti' ? project.peti : !project.peti);

      return matchesSearch && matchesStatus && matchesDependency && matchesPeti;
    });
  }, [filters, projectList]);

  const activeFilters = useMemo(() => {
    const chips = [];
    if (filters.query) chips.push({ key: 'query', label: `Busqueda: ${filters.query}` });
    if (filters.dependency !== 'all') chips.push({ key: 'dependency', label: `Dependencia: ${filters.dependency}` });
    if (filters.status !== 'all') chips.push({ key: 'status', label: `Estado: ${filters.status}` });
    if (filters.peti !== 'all') chips.push({ key: 'peti', label: filters.peti === 'peti' ? 'Solo PETI' : 'Solo NO PETI' });
    return chips;
  }, [filters]);

  const clearFilters = () => setFilters(DEFAULT_FILTERS);

  const updateFilter = (field, value) => {
    setFilters((current) => ({ ...current, [field]: value }));
  };

  return (
    <div className="projects-container">
      <header className="projects-header">
        <div className="projects-header__topbar">
          <div className="header-actions-group">
            <button className="btn-toolbar" onClick={fetchProjects} disabled={loading}>
              <RefreshCw size={18} />
              <span>{loading ? 'Actualizando' : 'Actualizar'}</span>
            </button>
            {canCreateProject && (
              <button className="btn-new-project" onClick={() => navigate('/proyectos/nuevo')}>
                <Plus size={18} />
                <span>Nuevo Proyecto</span>
              </button>
            )}
          </div>
          <div className="header-summary-chip">
            <Filter size={16} />
            <span>
              {filteredProjects.length} de {projectList.length} proyectos visibles
            </span>
          </div>
        </div>

        <div className="header-title-group">
          <h1>Proyectos TIC</h1>
          <p className="subtitle">Gestion y seguimiento de todos los proyectos</p>
        </div>
      </header>

      <section className="filters-section">
        <div className="search-box">
          <Search size={18} className="search-icon" />
          <input
            type="text"
            placeholder="Buscar por nombre, codigo, director o dependencia..."
            value={filters.query}
            onChange={(event) => updateFilter('query', event.target.value)}
          />
        </div>

        <div className="select-filters">
          <select
            className="filter-select"
            value={filters.dependency}
            onChange={(event) => updateFilter('dependency', event.target.value)}
          >
            <option value="all">Todas las dependencias</option>
            {dependencyOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>

          <select
            className="filter-select"
            value={filters.peti}
            onChange={(event) => updateFilter('peti', event.target.value)}
          >
            <option value="all">PETI y No PETI</option>
            <option value="peti">Solo PETI</option>
            <option value="no_peti">Solo No PETI</option>
          </select>

          <select
            className="filter-select"
            value={filters.status}
            onChange={(event) => updateFilter('status', event.target.value)}
          >
            <option value="all">Todos los estados</option>
            <option value="ACTIVO">Activo</option>
            <option value="CON_RETRASOS">Con retrasos</option>
            <option value="CERRADO">Cerrado</option>
          </select>
        </div>

        {activeFilters.length > 0 ? (
          <div className="active-filters">
            {activeFilters.map((chip) => (
              <button
                key={chip.key}
                type="button"
                className="filter-chip"
                onClick={() => {
                  if (chip.key === 'query') updateFilter('query', '');
                  if (chip.key === 'dependency') updateFilter('dependency', 'all');
                  if (chip.key === 'status') updateFilter('status', 'all');
                  if (chip.key === 'peti') updateFilter('peti', 'all');
                }}
              >
                {chip.label}
                <X size={13} />
              </button>
            ))}
            <button type="button" className="filter-chip filter-chip--clear" onClick={clearFilters}>
              Limpiar filtros
            </button>
          </div>
        ) : null}
      </section>

      {error ? (
        <div className="error-banner">
          {error}
          <button onClick={fetchProjects} className="btn-retry">
            Reintentar
          </button>
        </div>
      ) : null}

      <ProjectListTable projects={filteredProjects} loading={loading} />
    </div>
  );
};

export default ProjectsPage;
