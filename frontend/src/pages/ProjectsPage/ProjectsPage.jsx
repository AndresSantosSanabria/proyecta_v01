import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Download, Filter, Plus, RefreshCw, Search, X } from 'lucide-react';
import ProjectListTable from '../../components/features/projects/ProjectListTable';
import { useAuthContext } from '../../context/AuthContext';
import projectService from '../../services/projectService';
import reportService from '../../services/reportService';
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

const extractProjects = (value) => {
  const payload = value?.data?.data ?? value?.data ?? value;

  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.proyectos)) return payload.proyectos;

  return [];
};

const ProjectsPage = () => {
  const { hasRole, hasPermission, isAdminLocal, transversal, assignedProjects } = useAuthContext();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const canCreateProject = usePermission('PROYECTO:CREAR');
  const canEditProject = usePermission('PROYECTO:EDITAR');
  const navigate = useNavigate();
  const isDirectorProjectRole = hasRole('DIRECTOR_PROYECTO');
  const hasAssignedProjects = Array.isArray(assignedProjects) && assignedProjects.length > 0;
  const isAdminLike = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasRole('GESTOR_TIC')
    || hasPermission('SISTEMA:CONFIGURAR');
  const shouldUseAssignedProjects = (isDirectorProjectRole || hasAssignedProjects) && !isAdminLike;

  const loadProjects = async () => {
    try {
      setLoading(true);
      setError(null);
      const projectsData = shouldUseAssignedProjects
        ? await projectService.getMyProjects()
        : await projectService.getAllUnpaged();
      setProjects(extractProjects(projectsData));
    } catch (err) {
      console.error('Error fetching projects:', err);
      setProjects([]);
      setError(
        shouldUseAssignedProjects
          ? 'No fue posible cargar tus proyectos asignados. Revisa que el usuario tenga asignaciones activas.'
          : 'No se pudo establecer conexion con el servidor'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadProjects();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shouldUseAssignedProjects]);

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

  const triggerBlobDownload = (blob, fileName) => {
    if (!(blob instanceof Blob)) return;

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  const handleExportExcel = async () => {
    try {
      setExporting(true);
      const blob = await reportService.downloadPortafolioExcel({
        query: filters.query?.trim() || undefined,
        dependency: filters.dependency !== 'all' ? filters.dependency : undefined,
        status: filters.status !== 'all' ? filters.status : undefined,
        peti: filters.peti !== 'all' ? filters.peti : undefined,
      });
      triggerBlobDownload(blob, 'Consolidado Seguimiento Proyectos PETI.xlsx');
    } catch (err) {
      console.error('Error exporting projects Excel:', err);
      window.alert('No fue posible descargar el Excel de proyectos.');
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="projects-container">
      <header className="projects-header">
        <div className="projects-header__topbar">
          <div className="header-actions-group">
              <button className="btn-toolbar" onClick={loadProjects} disabled={loading}>
                <RefreshCw size={18} />
                <span>{loading ? 'Actualizando' : 'Actualizar'}</span>
              </button>
              <button className="btn-toolbar" onClick={handleExportExcel} disabled={loading || exporting}>
                <Download size={18} />
                <span>{exporting ? 'Exportando' : 'Exportar Excel'}</span>
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
          <span className="header-kicker">Vista operativa</span>
          <h1>{shouldUseAssignedProjects ? 'Mis Proyectos' : 'Proyectos TIC'}</h1>
          <p className="subtitle">
            {shouldUseAssignedProjects
              ? 'Solo se muestran los proyectos asignados a tu usuario.'
              : 'Gestion y seguimiento de todos los proyectos'}
          </p>
        </div>
      </header>

      <section className="filters-section">
        <div className="filters-title">
          <span>Filtros activos</span>
          <strong>{activeFilters.length} aplicados</strong>
        </div>

        <div className="search-box">
          <label className="filter-label" htmlFor="project-search">Buscar</label>
          <div className="search-box__input">
            <Search size={16} className="search-icon" />
            <input
              id="project-search"
              type="text"
              placeholder="Buscar por nombre, codigo..."
              value={filters.query}
              onChange={(event) => updateFilter('query', event.target.value)}
            />
          </div>
        </div>

        <div className="select-filters">
          <div className="filter-field">
            <label className="filter-label" htmlFor="project-dependency">Dependencia</label>
            <select
              id="project-dependency"
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
          </div>

          <div className="filter-field">
            <label className="filter-label" htmlFor="project-peti">PETI</label>
            <select
              id="project-peti"
              className="filter-select"
              value={filters.peti}
              onChange={(event) => updateFilter('peti', event.target.value)}
            >
              <option value="all">PETI y No PETI</option>
              <option value="peti">Solo PETI</option>
              <option value="no_peti">Solo No PETI</option>
            </select>
          </div>

          <div className="filter-field">
            <label className="filter-label" htmlFor="project-status">Estado</label>
            <select
              id="project-status"
              className="filter-select"
              value={filters.status}
              onChange={(event) => updateFilter('status', event.target.value)}
            >
              <option value="all">Todos los estados</option>
              <option value="PENDIENTE">Pendiente</option>
              <option value="ACTIVO">Activo</option>
              <option value="CON_RETRASOS">Con retrasos</option>
              <option value="CERRADO">Cerrado</option>
            </select>
          </div>
        </div>

        {activeFilters.length > 0 ? (
          <div className="active-filters-group">
            <div className="active-filters-label">Filtros aplicados</div>
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
          </div>
        ) : null}
      </section>

      {error ? (
        <div className="error-banner">
          {error}
          <button onClick={loadProjects} className="btn-retry">
            Reintentar
          </button>
        </div>
      ) : null}

      <section className="projects-list-section">
        <div className="projects-list-heading">
          <span>Proyectos TIC</span>
          <strong>{filteredProjects.length} de {projectList.length}</strong>
        </div>
        <ProjectListTable
          projects={filteredProjects}
          loading={loading}
          canEditProject={canEditProject && !shouldUseAssignedProjects}
        />
      </section>
    </div>
  );
};

export default ProjectsPage;
