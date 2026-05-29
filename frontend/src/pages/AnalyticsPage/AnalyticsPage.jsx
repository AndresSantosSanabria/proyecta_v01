import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Building2,
  Filter,
  RefreshCw,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  TrendingDown,
  TrendingUp,
  X,
} from 'lucide-react';
import analyticsService from '../../services/analyticsService';
import './AnalyticsPage.css';

const SORT_OPTIONS = [
  { value: 'avance_desc', label: 'Avance descendente' },
  { value: 'eficacia_desc', label: 'Eficacia descendente' },
  { value: 'eficiencia_desc', label: 'Eficiencia descendente' },
  { value: 'furag_desc', label: 'Cobertura FURAG descendente' },
  { value: 'mitigacion_desc', label: 'Mitigación descendente' },
  { value: 'atrasos_desc', label: 'Atrasos descendentes' },
  { value: 'nombre_asc', label: 'Nombre A-Z' },
];

const DEFAULT_FILTERS = {
  query: '',
  dependencia: 'ALL',
  peti: 'ALL',
  estado: 'ALL',
  sortBy: 'avance_desc',
};

const clampPercent = (value) => {
  const numeric = Number(value ?? 0);
  if (Number.isNaN(numeric)) return 0;
  return Math.max(0, Math.min(100, numeric));
};

const normalizeText = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();

const formatPercent = (value) => `${clampPercent(value).toFixed(2)}%`;

const formatNumber = (value) => {
  const numeric = Number(value ?? 0);
  if (Number.isNaN(numeric)) {
    return '0';
  }
  return numeric.toLocaleString('es-CO');
};

const average = (items, accessor) => {
  const values = items
    .map((item) => Number(accessor(item) ?? 0))
    .filter((value) => Number.isFinite(value));
  if (!values.length) {
    return 0;
  }
  return values.reduce((sum, value) => sum + value, 0) / values.length;
};

const toneClass = (value) => {
  const numeric = Number(value ?? 0);
  if (numeric >= 80) return 'analytics-tone analytics-tone--positive';
  if (numeric >= 50) return 'analytics-tone analytics-tone--warning';
  return 'analytics-tone analytics-tone--negative';
};

const chartColor = (index) => {
  const palette = ['#60a5fa', '#34d399', '#f59e0b', '#f97316', '#ef4444', '#a78bfa'];
  return palette[index % palette.length];
};

const compareProjects = (items, sortBy, getFuragCoverage) => {
  const sorted = [...items].sort((a, b) => {
    switch (sortBy) {
      case 'eficacia_desc':
        return clampPercent(b?.eficacia) - clampPercent(a?.eficacia);
      case 'eficiencia_desc':
        return clampPercent(b?.eficiencia) - clampPercent(a?.eficiencia);
      case 'furag_desc':
        return clampPercent(getFuragCoverage(b)) - clampPercent(getFuragCoverage(a));
      case 'mitigacion_desc':
        return clampPercent(b?.indiceMitigacion) - clampPercent(a?.indiceMitigacion);
      case 'atrasos_desc':
        return Number(b?.atrasados ?? 0) - Number(a?.atrasados ?? 0);
      case 'nombre_asc':
        return normalizeText(a?.nombre).localeCompare(normalizeText(b?.nombre), 'es');
      case 'avance_desc':
      default:
        return clampPercent(b?.avance) - clampPercent(a?.avance);
    }
  });

  return sorted;
};

const isPeti = (project) => Boolean(project?.peti);

const matchesState = (projectState, filterState) => {
  if (filterState === 'ALL') {
    return true;
  }
  return normalizeText(projectState) === normalizeText(filterState);
};

const matchesPeti = (project, filterPeti) => {
  if (filterPeti === 'ALL') {
    return true;
  }
  if (filterPeti === 'PETI') {
    return isPeti(project);
  }
  return !isPeti(project);
};

const AnalyticsPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);

  const loadAnalytics = async () => {
    try {
      setLoading(true);
      setError(null);
      const payload = await analyticsService.getPortfolio();
      setData(payload);
    } catch (err) {
      console.error('Error cargando analiticas del portafolio:', err);
      setError('No fue posible cargar las analíticas del portafolio.');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const bootstrap = async () => {
      await loadAnalytics();
    };

    bootstrap();
  }, []);

  const projects = useMemo(() => {
    return Array.isArray(data?.proyectos) ? data.proyectos : [];
  }, [data]);

  const furagProjects = useMemo(() => {
    return Array.isArray(data?.furag?.proyectos) ? data.furag.proyectos : [];
  }, [data]);

  const dependencyOptions = useMemo(() => {
    return [...new Set(projects.map((project) => project?.dependencia?.trim()).filter(Boolean))]
      .sort((left, right) => left.localeCompare(right, 'es'))
      .map((value) => ({ value, label: value }));
  }, [projects]);

  const statusOptions = useMemo(() => {
    return [...new Set(projects.map((project) => project?.estado?.trim()).filter(Boolean))]
      .sort((left, right) => left.localeCompare(right, 'es'))
      .map((value) => ({ value, label: value }));
  }, [projects]);

  const furagLookup = useMemo(
    () => new Map(furagProjects.map((item) => [normalizeText(item?.proyectoId), item])),
    [furagProjects]
  );

  const getFuragCoverage = useCallback(
    (project) => {
      const directValue = Number(project?.furagCobertura ?? 0);
      if (Number.isFinite(directValue) && directValue > 0) {
        return directValue;
      }
      const byId = furagLookup.get(normalizeText(project?.proyectoId));
      if (byId) {
        return Number(byId?.cobertura ?? 0);
      }
      return 0;
    },
    [furagLookup]
  );

  const filteredProjects = useMemo(() => {
    const query = normalizeText(filters.query);
    return projects.filter((project) => {
      const haystack = [
        project?.proyectoId,
        project?.nombre,
        project?.dependencia,
        project?.estrategia,
        project?.estado,
        project?.peti ? 'PETI' : 'NO PETI',
      ]
        .filter(Boolean)
        .join(' ');

      if (query && !normalizeText(haystack).includes(query)) {
        return false;
      }

      if (filters.dependencia !== 'ALL' && normalizeText(project?.dependencia) !== normalizeText(filters.dependencia)) {
        return false;
      }

      if (!matchesPeti(project, filters.peti)) {
        return false;
      }

      if (!matchesState(project?.estado, filters.estado)) {
        return false;
      }

      return true;
    });
  }, [filters.dependencia, filters.estado, filters.peti, filters.query, projects]);

  const filteredProjectsSorted = useMemo(() => {
    return compareProjects(filteredProjects, filters.sortBy, getFuragCoverage);
  }, [filters.sortBy, filteredProjects, getFuragCoverage]);

  const comparisonProjects = useMemo(() => {
    return filteredProjectsSorted.slice(0, 10);
  }, [filteredProjectsSorted]);

  const summary = useMemo(() => {
    const total = filteredProjects.length;
    const petiProjects = filteredProjects.filter((project) => isPeti(project)).length;
    const noPetiProjects = total - petiProjects;
    const avancePromedio = average(filteredProjects, (project) => project?.avance);
    const eficaciaPromedio = average(filteredProjects, (project) => project?.eficacia);
    const eficienciaPromedio = average(filteredProjects, (project) => project?.eficiencia);
    const furagPromedio = average(filteredProjects, (project) => getFuragCoverage(project));
    const mitigacionPromedio = average(filteredProjects, (project) => project?.indiceMitigacion);
    const enTiempo = filteredProjects.filter((project) => normalizeText(project?.estado).includes('tiempo')).length;
    const enAtraso = filteredProjects.filter((project) => normalizeText(project?.estado).includes('atraso')).length;

    return {
      total,
      petiProjects,
      noPetiProjects,
      avancePromedio,
      eficaciaPromedio,
      eficienciaPromedio,
      furagPromedio,
      mitigacionPromedio,
      enTiempo,
      enAtraso,
    };
  }, [filteredProjects, getFuragCoverage]);

  const dependencyBreakdown = useMemo(() => {
    const grouped = new Map();
    filteredProjects.forEach((project) => {
      const key = project?.dependencia?.trim() || 'Sin dependencia';
      const normalizedKey = normalizeText(key);
      const current = grouped.get(normalizedKey) ?? {
        key: normalizedKey,
        dependencia: key,
        totalProyectos: 0,
        avanceSum: 0,
        eficaciaSum: 0,
        eficienciaSum: 0,
        furagSum: 0,
        mitigacionSum: 0,
        petiCount: 0,
      };

      current.totalProyectos += 1;
      current.avanceSum += clampPercent(project?.avance);
      current.eficaciaSum += clampPercent(project?.eficacia);
      current.eficienciaSum += clampPercent(project?.eficiencia);
      current.furagSum += clampPercent(getFuragCoverage(project));
      current.mitigacionSum += clampPercent(project?.indiceMitigacion);
      current.petiCount += isPeti(project) ? 1 : 0;
      grouped.set(normalizedKey, current);
    });

    return [...grouped.values()]
      .map((item) => ({
        ...item,
        avancePromedio: item.totalProyectos ? item.avanceSum / item.totalProyectos : 0,
        eficaciaPromedio: item.totalProyectos ? item.eficaciaSum / item.totalProyectos : 0,
        eficienciaPromedio: item.totalProyectos ? item.eficienciaSum / item.totalProyectos : 0,
        furagPromedio: item.totalProyectos ? item.furagSum / item.totalProyectos : 0,
        mitigacionPromedio: item.totalProyectos ? item.mitigacionSum / item.totalProyectos : 0,
      }))
      .sort((left, right) => right.avancePromedio - left.avancePromedio);
  }, [filteredProjects, getFuragCoverage]);

  const petiBreakdown = useMemo(() => {
    const petiProjects = filteredProjects.filter((project) => isPeti(project));
    const noPetiProjects = filteredProjects.filter((project) => !isPeti(project));
    const total = filteredProjects.length || 1;

    return [
      {
        codigo: 'PETI',
        nombre: 'PETI',
        proyectos: petiProjects.length,
        avancePromedio: average(petiProjects, (project) => project?.avance),
        eficaciaPromedio: average(petiProjects, (project) => project?.eficacia),
        eficienciaPromedio: average(petiProjects, (project) => project?.eficiencia),
        width: (petiProjects.length / total) * 100,
        color: '#60a5fa',
      },
      {
        codigo: 'NO_PETI',
        nombre: 'NO PETI',
        proyectos: noPetiProjects.length,
        avancePromedio: average(noPetiProjects, (project) => project?.avance),
        eficaciaPromedio: average(noPetiProjects, (project) => project?.eficacia),
        eficienciaPromedio: average(noPetiProjects, (project) => project?.eficiencia),
        width: (noPetiProjects.length / total) * 100,
        color: '#34d399',
      },
    ].filter((item) => item.proyectos > 0);
  }, [filteredProjects]);

  const visibleRiskText = useMemo(() => {
    const total = Number(data?.riesgos?.total ?? 0);
    const tratados = Number(data?.riesgos?.tratados ?? 0);
    return `${formatNumber(tratados)} tratados de ${formatNumber(total)}`;
  }, [data]);

  const activeFilterChips = useMemo(() => {
    const chips = [];
    if (filters.query) chips.push({ key: 'query', label: `Búsqueda: ${filters.query}` });
    if (filters.dependencia !== 'ALL') chips.push({ key: 'dependencia', label: `Dependencia: ${filters.dependencia}` });
    if (filters.peti !== 'ALL') chips.push({ key: 'peti', label: filters.peti === 'PETI' ? 'Solo PETI' : 'Solo NO PETI' });
    if (filters.estado !== 'ALL') chips.push({ key: 'estado', label: `Estado: ${filters.estado}` });
    return chips;
  }, [filters]);

  const updateFilter = (field, value) => {
    setFilters((current) => ({ ...current, [field]: value }));
  };

  const resetFilters = () => {
    setFilters(DEFAULT_FILTERS);
  };

  return (
    <div className="analytics-page">
      <section className="analytics-hero">
        <div>
          <span className="analytics-kicker">Gobernanza analítica</span>
          <h1>Analíticas del Portafolio</h1>
          <p>
            Tablero comparativo tipo Power BI para leer desempeño por proyecto, dependencia y clasificación PETI,
            con filtros que reaccionan en todos los visuales.
          </p>
        </div>

        <div className="analytics-hero__actions">
          <button className="analytics-refresh" onClick={loadAnalytics} disabled={loading}>
            <RefreshCw size={18} />
            {loading ? 'Actualizando' : 'Actualizar'}
          </button>
          <button className="analytics-refresh analytics-refresh--secondary" onClick={resetFilters}>
            <X size={18} />
            Limpiar filtros
          </button>
        </div>
      </section>

      {error ? (
        <div className="analytics-alert">
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      ) : null}

      <section className="analytics-panel analytics-filters">
        <div className="analytics-panel__header analytics-panel__header--filters">
          <div>
            <h2>Filtros interactivos</h2>
            <p>Slicers que cruzan toda la página al estilo de un tablero ejecutivo.</p>
          </div>
          <div className="analytics-panel__meta">
            <span>
              <Filter size={14} /> {activeFilterChips.length} filtros activos
            </span>
            <span>
              <Search size={14} /> {formatNumber(filteredProjects.length)} de {formatNumber(projects.length)} proyectos
            </span>
          </div>
        </div>

        <div className="analytics-filters__grid">
          <label className="analytics-filter">
            <span>
              <Search size={14} /> Buscar proyecto
            </span>
            <input
              type="search"
              value={filters.query}
              onChange={(event) => updateFilter('query', event.target.value)}
              placeholder="Nombre, código, dependencia..."
            />
          </label>

          <label className="analytics-filter">
            <span>
              <Building2 size={14} /> Dependencia
            </span>
            <select
              value={filters.dependencia}
              onChange={(event) => updateFilter('dependencia', event.target.value)}
            >
              <option value="ALL">Todas las dependencias</option>
              {dependencyOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="analytics-filter">
            <span>
              <Sparkles size={14} /> Clasificación
            </span>
            <select
              value={filters.peti}
              onChange={(event) => updateFilter('peti', event.target.value)}
            >
              <option value="ALL">PETI y NO PETI</option>
              <option value="PETI">Solo PETI</option>
              <option value="NO_PETI">Solo NO PETI</option>
            </select>
          </label>

          <label className="analytics-filter">
            <span>
              <Activity size={14} /> Estado
            </span>
            <select value={filters.estado} onChange={(event) => updateFilter('estado', event.target.value)}>
              <option value="ALL">Todos los estados</option>
              {statusOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <label className="analytics-filter">
            <span>
              <SlidersHorizontal size={14} /> Ordenar por
            </span>
            <select value={filters.sortBy} onChange={(event) => updateFilter('sortBy', event.target.value)}>
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>

          <div className="analytics-filter analytics-filter--summary">
            <span>
              <Sparkles size={14} /> Resumen rápido
            </span>
            <strong>
              {formatNumber(summary.total)} proyectos filtrados
            </strong>
            <small>
              {formatNumber(summary.petiProjects)} PETI · {formatNumber(summary.noPetiProjects)} NO PETI
            </small>
          </div>
        </div>

        {activeFilterChips.length > 0 ? (
          <div className="analytics-active-filters">
            {activeFilterChips.map((chip) => (
              <button
                key={chip.key}
                type="button"
                className="analytics-chip"
                onClick={() => {
                  if (chip.key === 'query') updateFilter('query', '');
                  if (chip.key === 'dependencia') updateFilter('dependencia', 'ALL');
                  if (chip.key === 'peti') updateFilter('peti', 'ALL');
                  if (chip.key === 'estado') updateFilter('estado', 'ALL');
                }}
              >
                {chip.label}
                <X size={13} />
              </button>
            ))}
          </div>
        ) : null}
      </section>

      <section className="analytics-kpis analytics-kpis--wide">
        <article className="analytics-kpi-card">
          <span>Proyectos filtrados</span>
          <strong>{formatNumber(summary.total)}</strong>
          <small>
            {formatNumber(summary.enTiempo)} en tiempo · {formatNumber(summary.enAtraso)} en atraso
          </small>
        </article>
        <article className="analytics-kpi-card">
          <span>PETI</span>
          <strong>{formatNumber(summary.petiProjects)}</strong>
          <small>Proyectos estratégicos</small>
        </article>
        <article className="analytics-kpi-card">
          <span>No PETI</span>
          <strong>{formatNumber(summary.noPetiProjects)}</strong>
          <small>Iniciativas operativas</small>
        </article>
        <article className="analytics-kpi-card">
          <span>Avance promedio</span>
          <strong>{formatPercent(summary.avancePromedio)}</strong>
          <small>Promedio del filtro activo</small>
        </article>
        <article className="analytics-kpi-card">
          <span>Eficacia promedio</span>
          <strong className={toneClass(summary.eficaciaPromedio)}>{formatPercent(summary.eficaciaPromedio)}</strong>
          <small>Programado versus ejecutado</small>
        </article>
        <article className="analytics-kpi-card">
          <span>Eficiencia promedio</span>
          <strong className={toneClass(summary.eficienciaPromedio)}>{formatPercent(summary.eficienciaPromedio)}</strong>
          <small>Entrega a tiempo sobre lo entregado</small>
        </article>
        <article className="analytics-kpi-card">
          <span>Cobertura FURAG</span>
          <strong>{formatPercent(summary.furagPromedio)}</strong>
          <small>{visibleRiskText}</small>
        </article>
        <article className="analytics-kpi-card">
          <span>Mitigación de riesgos</span>
          <strong>{formatPercent(summary.mitigacionPromedio)}</strong>
          <small>{formatNumber(data?.riesgos?.tratados)} tratados en total</small>
        </article>
      </section>

      <section className="analytics-panel">
        <div className="analytics-panel__header">
          <div>
            <h2>Comparativa entre proyectos</h2>
            <p>Ranking comparativo por avance, eficacia y eficiencia con el filtro activo.</p>
          </div>
          <div className="analytics-panel__meta">
            <span>
              <Sparkles size={14} /> Corte {data?.corte ?? 'hoy'}
            </span>
            <span>
              <TrendingUp size={14} /> {formatNumber(summary.enTiempo)} en tiempo
            </span>
            <span>
              <TrendingDown size={14} /> {formatNumber(summary.enAtraso)} en atraso
            </span>
          </div>
        </div>

        <div className="project-bars">
          {comparisonProjects.length === 0 ? (
            <div className="analytics-empty">No hay proyectos que coincidan con los filtros.</div>
          ) : (
            comparisonProjects.map((project, index) => (
              <div key={project.proyectoId} className="project-bars__row">
                <div className="project-bars__meta">
                  <strong>{project.nombre}</strong>
                  <span>{project.dependencia}</span>
                  <small>
                    {isPeti(project) ? 'PETI' : 'NO PETI'} · {project.estado ?? 'Sin estado'}
                  </small>
                </div>
                <div className="project-bars__chart">
                  <div className="project-bars__track">
                    <span
                      style={{
                        width: `${clampPercent(project.avance)}%`,
                        background: `linear-gradient(90deg, ${chartColor(index)}, color-mix(in srgb, ${chartColor(index)} 60%, var(--primary)))`,
                      }}
                    />
                  </div>
                  <div className="project-bars__track project-bars__track--secondary">
                    <span
                      style={{
                        width: `${clampPercent(project.eficacia)}%`,
                        background:
                          'linear-gradient(90deg, var(--success), color-mix(in srgb, var(--success) 55%, var(--primary)))',
                      }}
                    />
                  </div>
                  <div className="project-bars__track project-bars__track--tertiary">
                    <span
                      style={{
                        width: `${clampPercent(project.eficiencia)}%`,
                        background:
                          'linear-gradient(90deg, var(--warning), color-mix(in srgb, var(--warning) 55%, var(--primary)))',
                      }}
                    />
                  </div>
                </div>
                <div className="project-bars__values">
                  <span>Avance {formatPercent(project.avance)}</span>
                  <span>Eficacia {formatPercent(project.eficacia)}</span>
                  <span>Eficiencia {formatPercent(project.eficiencia)}</span>
                </div>
              </div>
            ))
          )}
        </div>
      </section>

      <section className="analytics-grid analytics-grid--powerbi">
        <article className="analytics-card analytics-card--wide">
          <div className="analytics-card__title">
            <h3>Distribución por dependencia</h3>
            <span>{formatNumber(dependencyBreakdown.length)} dependencias</span>
          </div>
          <div className="analytics-bars">
            {dependencyBreakdown.length === 0 ? (
              <div className="analytics-empty">No hay dependencias disponibles para el filtro actual.</div>
            ) : (
              dependencyBreakdown.map((item, index) => (
                <button
                  key={item.key}
                  type="button"
                  className="analytics-bar analytics-bar--button"
                  onClick={() => updateFilter('dependencia', item.dependencia)}
                >
                  <div className="analytics-bar__head">
                    <strong>{item.dependencia}</strong>
                    <span>{formatPercent(item.avancePromedio)}</span>
                  </div>
                  <div className="analytics-bar__track">
                    <span
                      style={{
                        width: `${clampPercent(item.avancePromedio)}%`,
                        background: `linear-gradient(90deg, ${chartColor(index)}, color-mix(in srgb, ${chartColor(index)} 65%, var(--success)))`,
                      }}
                    />
                  </div>
                  <div className="analytics-bar__foot">
                    <small>{formatNumber(item.totalProyectos)} proyectos</small>
                    <small>{formatPercent(item.furagPromedio)} FURAG</small>
                    <small>{formatPercent(item.mitigacionPromedio)} mitigación</small>
                  </div>
                </button>
              ))
            )}
          </div>
        </article>

        <article className="analytics-card">
          <div className="analytics-card__title">
            <h3>Clasificación PETI</h3>
            <span>{petiBreakdown.length} categorías</span>
          </div>
          <div className="strategy-stack">
            {petiBreakdown.length === 0 ? (
              <div className="analytics-empty">No hay clasificación PETI para el filtro activo.</div>
            ) : (
              <>
                <div className="strategy-stack__rail">
                  {petiBreakdown.map((item) => (
                    <button
                      key={item.codigo}
                      type="button"
                      className="strategy-stack__segment strategy-stack__segment--button"
                      style={{
                        flex: `${Math.max(item.width, 1)} 1 0%`,
                        background: item.color,
                      }}
                      onClick={() => updateFilter('peti', item.codigo)}
                    >
                      <span>{item.nombre}</span>
                    </button>
                  ))}
                </div>
                <div className="strategy-stack__legend">
                  {petiBreakdown.map((item) => (
                    <button
                      key={item.codigo}
                      type="button"
                      className="strategy-stack__legend-item strategy-stack__legend-item--button"
                      onClick={() => updateFilter('peti', item.codigo)}
                    >
                      <span style={{ background: item.color }} />
                      <strong>{item.nombre}</strong>
                      <small>
                        {formatNumber(item.proyectos)} proyectos · {formatPercent(item.avancePromedio)} avance ·{' '}
                        {formatPercent(item.eficienciaPromedio)} eficiencia
                      </small>
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
        </article>
      </section>

      <section className="analytics-panel">
        <div className="analytics-panel__header">
          <div>
            <h2>Matriz comparativa</h2>
            <p>Lectura tabular tipo Power BI con métricas filtradas por proyecto.</p>
          </div>
          <div className="analytics-panel__meta">
            <span>
              <BarChart3 size={14} /> {formatNumber(filteredProjects.length)} filas
            </span>
            <span>
              <ShieldCheck size={14} /> {formatPercent(summary.mitigacionPromedio)} mitigación promedio
            </span>
          </div>
        </div>

        <div className="analytics-matrix">
          {filteredProjectsSorted.length === 0 ? (
            <div className="analytics-empty">No hay filas para mostrar con los filtros actuales.</div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Proyecto</th>
                  <th>Dependencia</th>
                  <th>Clasificación</th>
                  <th>Avance</th>
                  <th>Eficacia</th>
                  <th>Eficiencia</th>
                  <th>FURAG</th>
                  <th>Mitigación</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {filteredProjectsSorted.map((project, index) => {
                  const furagCoverage = getFuragCoverage(project);
                  const tone = normalizeText(project?.estado).includes('atras')
                    ? 'analytics-matrix__state analytics-matrix__state--danger'
                    : 'analytics-matrix__state analytics-matrix__state--success';

                  return (
                    <tr key={project.proyectoId} className={index % 2 === 0 ? 'analytics-matrix__row--alt' : undefined}>
                      <td>
                        <strong>{project.nombre}</strong>
                        <small>{project.proyectoId}</small>
                      </td>
                      <td>{project.dependencia}</td>
                      <td>
                        <span className={isPeti(project) ? 'analytics-pill analytics-pill--primary' : 'analytics-pill'}>
                          {isPeti(project) ? 'PETI' : 'NO PETI'}
                        </span>
                      </td>
                      <td>
                        <div className="analytics-cell-metric">
                          <strong>{formatPercent(project.avance)}</strong>
                          <div className="analytics-cell-track">
                            <span style={{ width: `${clampPercent(project.avance)}%` }} />
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="analytics-cell-metric">
                          <strong>{formatPercent(project.eficacia)}</strong>
                          <div className="analytics-cell-track">
                            <span
                              style={{
                                width: `${clampPercent(project.eficacia)}%`,
                                background: 'linear-gradient(90deg, var(--success), color-mix(in srgb, var(--success) 55%, var(--primary)))',
                              }}
                            />
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="analytics-cell-metric">
                          <strong>{formatPercent(project.eficiencia)}</strong>
                          <div className="analytics-cell-track">
                            <span
                              style={{
                                width: `${clampPercent(project.eficiencia)}%`,
                                background: 'linear-gradient(90deg, var(--warning), color-mix(in srgb, var(--warning) 55%, var(--primary)))',
                              }}
                            />
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="analytics-cell-metric">
                          <strong>{formatPercent(furagCoverage)}</strong>
                          <div className="analytics-cell-track">
                            <span
                              style={{
                                width: `${clampPercent(furagCoverage)}%`,
                                background: 'linear-gradient(90deg, #8b5cf6, color-mix(in srgb, #8b5cf6 55%, var(--primary)))',
                              }}
                            />
                          </div>
                        </div>
                      </td>
                      <td>
                        <div className="analytics-cell-metric">
                          <strong>{formatPercent(project.indiceMitigacion)}</strong>
                          <div className="analytics-cell-track">
                            <span
                              style={{
                                width: `${clampPercent(project.indiceMitigacion)}%`,
                                background: 'linear-gradient(90deg, #22c55e, color-mix(in srgb, #22c55e 55%, var(--primary)))',
                              }}
                            />
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className={tone}>{project.estado ?? 'Sin estado'}</span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </section>

      {loading ? <div className="analytics-loading">Cargando analíticas...</div> : null}
    </div>
  );
};

export default AnalyticsPage;
