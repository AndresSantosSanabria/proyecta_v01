import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Building2,
  Calendar,
  ChevronRight,
  Download,
  RefreshCw,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Target,
  TrendingDown,
  TrendingUp,
  X,
  Zap,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  PolarAngleAxis,
  PolarGrid,
  PolarRadiusAxis,
  Radar,
  RadarChart,
  ResponsiveContainer,
  Scatter,
  ScatterChart,
  Tooltip,
  XAxis,
  YAxis,
  ZAxis,
  Pie,
  PieChart,
} from 'recharts';
import analyticsService from '../../services/analyticsService';
import { AutocompleteSelect } from '../../components/common/AutocompleteSelect';
import './AnalyticsPage.css';

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Constants â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
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
  fechaInicio: '',
  fechaFin: '',
};

const CHART_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#f97316', '#84cc16'];
const CHART_COLORS_PASTEL = ['#60a5fa', '#34d399', '#fcd34d', '#f87171', '#a78bfa', '#67e8f9'];
const COMPARISON_PAGE_SIZE = 3;

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
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

const formatPercent = (value) => `${clampPercent(value).toFixed(1)}%`;

const formatNumber = (value) => {
  const numeric = Number(value ?? 0);
  if (Number.isNaN(numeric)) return '0';
  return numeric.toLocaleString('es-CO');
};

const average = (items, accessor) => {
  const values = items
    .map((item) => Number(accessor(item) ?? 0))
    .filter((value) => Number.isFinite(value));
  if (!values.length) return 0;
  return values.reduce((sum, value) => sum + value, 0) / values.length;
};

const toneClass = (value) => {
  const numeric = Number(value ?? 0);
  if (numeric >= 80) return 'analytics-tone analytics-tone--positive';
  if (numeric >= 50) return 'analytics-tone analytics-tone--warning';
  return 'analytics-tone analytics-tone--negative';
};

const compareProjects = (items, sortBy, getFuragCoverage) => {
  return [...items].sort((a, b) => {
    switch (sortBy) {
      case 'eficacia_desc': return clampPercent(b?.eficacia) - clampPercent(a?.eficacia);
      case 'eficiencia_desc': return clampPercent(b?.eficiencia) - clampPercent(a?.eficiencia);
      case 'furag_desc': return clampPercent(getFuragCoverage(b)) - clampPercent(getFuragCoverage(a));
      case 'mitigacion_desc': return clampPercent(b?.indiceMitigacion) - clampPercent(a?.indiceMitigacion);
      case 'atrasos_desc': return Number(b?.atrasados ?? 0) - Number(a?.atrasados ?? 0);
      case 'nombre_asc': return normalizeText(a?.nombre).localeCompare(normalizeText(b?.nombre), 'es');
      case 'avance_desc':
      default: return clampPercent(b?.avance) - clampPercent(a?.avance);
    }
  });
};

const isPeti = (project) => Boolean(project?.peti);

const matchesState = (projectState, filterState) => {
  if (filterState === 'ALL') return true;
  return normalizeText(projectState) === normalizeText(filterState);
};

const matchesPeti = (project, filterPeti) => {
  if (filterPeti === 'ALL') return true;
  if (filterPeti === 'PETI') return isPeti(project);
  return !isPeti(project);
};

const isInDateRange = (project, fechaInicio, fechaFin) => {
  if (!fechaInicio && !fechaFin) return true;
  const fechaInicioProject = project?.fechaInicio ?? project?.fecha_inicio ?? project?.startDate;
  const fechaFinProject = project?.fechaFin ?? project?.fecha_fin ?? project?.endDate;
  const inicio = fechaInicio ? new Date(fechaInicio) : null;
  const fin = fechaFin ? new Date(fechaFin) : null;
  const pInicio = fechaInicioProject ? new Date(fechaInicioProject) : null;
  const pFin = fechaFinProject ? new Date(fechaFinProject) : null;

  if (inicio && pFin && pFin < inicio) return false;
  if (fin && pInicio && pInicio > fin) return false;
  return true;
};

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Custom Tooltip Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
const ScatterTooltipContent = ({ active, payload }) => {
  if (!active || !payload?.length) return null;
  const d = payload[0]?.payload;
  return (
    <div className="chart-tooltip">
      <strong className="chart-tooltip__title">{d?.nombre ?? 'Proyecto'}</strong>
      <div className="chart-tooltip__row"><span>Eficiencia</span><span>{formatPercent(d?.x)}</span></div>
      <div className="chart-tooltip__row"><span>Eficacia</span><span>{formatPercent(d?.y)}</span></div>
      <div className="chart-tooltip__row"><span>Avance</span><span>{formatPercent(d?.z)}</span></div>
      {d?.dependencia && <div className="chart-tooltip__row"><span>Dependencia</span><span>{d.dependencia}</span></div>}
    </div>
  );
};

const BarTooltipContent = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="chart-tooltip">
      <strong className="chart-tooltip__title">{label}</strong>
      {payload.map((entry) => (
        <div key={entry.dataKey} className="chart-tooltip__row">
          <span style={{ color: entry.color }}>{entry.name}</span>
          <span>{formatPercent(entry.value)}</span>
        </div>
      ))}
    </div>
  );
};

const DonutTooltipContent = ({ active, payload }) => {
  if (!active || !payload?.length) return null;
  const d = payload[0];
  return (
    <div className="chart-tooltip">
      <strong className="chart-tooltip__title">{d?.name}</strong>
      <div className="chart-tooltip__row"><span>Proyectos</span><span>{d?.value}</span></div>
      <div className="chart-tooltip__row"><span>Porcentaje</span><span>{formatPercent(d?.payload?.percent * 100)}</span></div>
    </div>
  );
};

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Custom Donut Label â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
const renderCustomLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }) => {
  if (percent < 0.05) return null;
  const RADIAN = Math.PI / 180;
  const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="white" textAnchor="middle" dominantBaseline="central" fontSize={12} fontWeight={700}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

/* â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ Main Component â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
const AnalyticsPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [comparisonPage, setComparisonPage] = useState(1);

  const loadAnalytics = async () => {
    try {
      setLoading(true);
      setError(null);
      const payload = await analyticsService.getPortfolio();
      setData(payload);
    } catch (err) {
      console.error('Error cargando analíticas del portafolio:', err);
      setError('No fue posible cargar las analíticas del portafolio.');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  const [downloadingPdf, setDownloadingPdf] = useState(false);

  const handleDownloadPdf = async () => {
    try {
      setDownloadingPdf(true);
      const blob = await analyticsService.downloadPortfolioPdf();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'analitica-portafolio.pdf';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Error descargando PDF:', err);
      setError('No fue posible generar el reporte PDF.');
    } finally {
      setDownloadingPdf(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadAnalytics();
  }, []);

  const projects = useMemo(() => Array.isArray(data?.proyectos) ? data.proyectos : [], [data]);

  const furagProjects = useMemo(() => Array.isArray(data?.furag?.proyectos) ? data.furag.proyectos : [], [data]);

  const dependencyOptions = useMemo(() =>
    [...new Set(projects.map((p) => p?.dependencia?.trim()).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b, 'es'))
      .map((v) => ({ value: v, label: v })),
    [projects]
  );

  const statusOptions = useMemo(() =>
    [...new Set(projects.map((p) => p?.estado?.trim()).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b, 'es'))
      .map((v) => ({ value: v, label: v })),
    [projects]
  );

  const furagLookup = useMemo(
    () => new Map(furagProjects.map((item) => [normalizeText(item?.proyectoId), item])),
    [furagProjects]
  );

  const getFuragCoverage = useCallback(
    (project) => {
      const directValue = Number(project?.furagCobertura ?? 0);
      if (Number.isFinite(directValue) && directValue > 0) return directValue;
      const byId = furagLookup.get(normalizeText(project?.proyectoId));
      if (byId) return Number(byId?.cobertura ?? 0);
      return 0;
    },
    [furagLookup]
  );

  const filteredProjects = useMemo(() => {
    const query = normalizeText(filters.query);
    return projects.filter((project) => {
      const haystack = [project?.proyectoId, project?.nombre, project?.dependencia, project?.estrategia, project?.estado, project?.peti ? 'PETI' : 'NO PETI']
        .filter(Boolean).join(' ');
      if (query && !normalizeText(haystack).includes(query)) return false;
      if (filters.dependencia !== 'ALL' && normalizeText(project?.dependencia) !== normalizeText(filters.dependencia)) return false;
      if (!matchesPeti(project, filters.peti)) return false;
      if (!matchesState(project?.estado, filters.estado)) return false;
      if (!isInDateRange(project, filters.fechaInicio, filters.fechaFin)) return false;
      return true;
    });
  }, [filters, projects]);

  const filteredProjectsSorted = useMemo(() =>
    compareProjects(filteredProjects, filters.sortBy, getFuragCoverage),
    [filters.sortBy, filteredProjects, getFuragCoverage]
  );

  const comparisonProjects = useMemo(() => filteredProjectsSorted.slice(0, 10), [filteredProjectsSorted]);
  const comparisonTotalPages = Math.max(1, Math.ceil(comparisonProjects.length / COMPARISON_PAGE_SIZE));
  const comparisonPageSafe = Math.min(comparisonPage, comparisonTotalPages);
  const comparisonProjectsPage = useMemo(() => {
    const start = (comparisonPageSafe - 1) * COMPARISON_PAGE_SIZE;
    return comparisonProjects.slice(start, start + COMPARISON_PAGE_SIZE);
  }, [comparisonPageSafe, comparisonProjects]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setComparisonPage(1);
  }, [filters.sortBy, filteredProjects.length]);

  const summary = useMemo(() => {
    const total = filteredProjects.length;
    const petiProjects = filteredProjects.filter(isPeti).length;
    const noPetiProjects = total - petiProjects;
    const avancePromedio = average(filteredProjects, (p) => p?.avance);
    const eficaciaPromedio = average(filteredProjects, (p) => p?.eficacia);
    const eficienciaPromedio = average(filteredProjects, (p) => p?.eficiencia);
    const furagPromedio = average(filteredProjects, (p) => getFuragCoverage(p));
    const mitigacionPromedio = average(filteredProjects, (p) => p?.indiceMitigacion);
    const enTiempo = filteredProjects.filter((p) => normalizeText(p?.estado).includes('tiempo')).length;
    const enAtraso = filteredProjects.filter((p) => normalizeText(p?.estado).includes('atraso')).length;
    const saludPortafolio = total > 0 ? (enTiempo / total) * 100 : 0;
    return { total, petiProjects, noPetiProjects, avancePromedio, eficaciaPromedio, eficienciaPromedio, furagPromedio, mitigacionPromedio, enTiempo, enAtraso, saludPortafolio };
  }, [filteredProjects, getFuragCoverage]);

  /* â”€â”€â”€ Chart Data â”€â”€â”€ */
  const scatterData = useMemo(() =>
    filteredProjects.map((p) => ({
      x: clampPercent(p?.eficiencia),
      y: clampPercent(p?.eficacia),
      z: clampPercent(p?.avance),
      nombre: p?.nombre ?? p?.proyectoId,
      dependencia: p?.dependencia,
      estado: p?.estado,
    })),
    [filteredProjects]
  );

  const barByDependencyData = useMemo(() => {
    const grouped = new Map();
    filteredProjects.forEach((p) => {
      const key = p?.dependencia?.trim() || 'Sin dependencia';
      const cur = grouped.get(key) ?? { dependencia: key, avance: 0, eficacia: 0, eficiencia: 0, count: 0 };
      cur.avance += clampPercent(p?.avance);
      cur.eficacia += clampPercent(p?.eficacia);
      cur.eficiencia += clampPercent(p?.eficiencia);
      cur.count += 1;
      grouped.set(key, cur);
    });
    return [...grouped.values()]
      .map((item) => ({
        name: item.dependencia.length > 18 ? item.dependencia.slice(0, 18) + '…' : item.dependencia,
        fullName: item.dependencia,
        Avance: Math.round(item.avance / item.count),
        Eficacia: Math.round(item.eficacia / item.count),
        Eficiencia: Math.round(item.eficiencia / item.count),
        proyectos: item.count,
      }))
      .sort((a, b) => b.Avance - a.Avance)
      .slice(0, 8);
  }, [filteredProjects]);

  const donutData = useMemo(() => {
    const groups = new Map();
    filteredProjects.forEach((p) => {
      const estado = p?.estado?.trim() || 'Sin estado';
      groups.set(estado, (groups.get(estado) ?? 0) + 1);
    });
    return [...groups.entries()]
      .map(([name, value]) => ({ name, value }))
      .sort((a, b) => b.value - a.value);
  }, [filteredProjects]);

  const radarData = useMemo(() => {
    if (!filteredProjects.length) return [];
    const portAvg = {
      Avance: Math.round(summary.avancePromedio),
      Eficacia: Math.round(summary.eficaciaPromedio),
      Eficiencia: Math.round(summary.eficienciaPromedio),
      FURAG: Math.round(summary.furagPromedio),
      Mitigacion: Math.round(summary.mitigacionPromedio),
    };
    return [
      { metric: 'Avance', Portafolio: portAvg.Avance },
      { metric: 'Eficacia', Portafolio: portAvg.Eficacia },
      { metric: 'Eficiencia', Portafolio: portAvg.Eficiencia },
      { metric: 'FURAG', Portafolio: portAvg.FURAG },
      { metric: 'Mitigacion', Portafolio: portAvg.Mitigacion },
    ];
  }, [filteredProjects, summary]);

  const activeFilterChips = useMemo(() => {
    const chips = [];
    if (filters.query) chips.push({ key: 'query', label: `Búsqueda: ${filters.query}` });
    if (filters.dependencia !== 'ALL') chips.push({ key: 'dependencia', label: `Dependencia: ${filters.dependencia}` });
    if (filters.peti !== 'ALL') chips.push({ key: 'peti', label: filters.peti === 'PETI' ? 'Solo PETI' : 'Solo NO PETI' });
    if (filters.estado !== 'ALL') chips.push({ key: 'estado', label: `Estado: ${filters.estado}` });
    if (filters.fechaInicio) chips.push({ key: 'fechaInicio', label: `Desde: ${filters.fechaInicio}` });
    if (filters.fechaFin) chips.push({ key: 'fechaFin', label: `Hasta: ${filters.fechaFin}` });
    return chips;
  }, [filters]);

  const updateFilter = (field, value) => setFilters((cur) => ({ ...cur, [field]: value }));
  const resetFilters = () => setFilters(DEFAULT_FILTERS);

  const visibleRiskText = useMemo(() => {
    const total = Number(data?.riesgos?.total ?? 0);
    const tratados = Number(data?.riesgos?.tratados ?? 0);
    return `${formatNumber(tratados)} tratados de ${formatNumber(total)}`;
  }, [data]);

  /* â”€â”€â”€ Recharts shared axis tick style â”€â”€â”€ */
  const axisStyle = { fontSize: 11, fill: 'var(--text-muted)' };

  return (
    <div className="analytics-page">
      {/* â”€â”€ Hero â”€â”€ */}
      <section className="analytics-hero">
        <div>
          <span className="analytics-kicker">Gobernanza analítica</span>
          <h1>Analíticas del Portafolio</h1>
          <p>
            Tablero ejecutivo interactivo con KPIs de eficiencia y eficacia, gráficos avanzados y filtros
            que reaccionan en todos los visuales.
          </p>
        </div>
        <div className="analytics-hero__actions">
          <button className="analytics-refresh" onClick={loadAnalytics} disabled={loading}>
            <RefreshCw size={18} />
            {loading ? 'Actualizando…' : 'Actualizar'}
          </button>
          <button className="analytics-refresh analytics-refresh--download" onClick={handleDownloadPdf} disabled={downloadingPdf || loading}>
            <Download size={18} />
            {downloadingPdf ? 'Generando…' : 'Descargar'}
          </button>
          <button className="analytics-refresh analytics-refresh--secondary" onClick={resetFilters}>
            <X size={18} /> Limpiar filtros
          </button>
        </div>
      </section>

      {/* â”€â”€ Error â”€â”€ */}
      {error ? (
        <div className="analytics-alert">
          <AlertTriangle size={18} /><span>{error}</span>
        </div>
      ) : null}

      {/* â”€â”€ Filters â”€â”€ */}
      <section className="analytics-panel analytics-filters">
        <div className="analytics-panel__header analytics-panel__header--filters">
          <div>
            <h2>Filtros interactivos</h2>
            <p>Slicers que cruzan todos los visuales al estilo de un tablero ejecutivo.</p>
          </div>
          <div className="analytics-panel__meta">
            <span><SlidersHorizontal size={14} /> {activeFilterChips.length} activos</span>
            <span><Search size={14} /> {formatNumber(filteredProjects.length)} / {formatNumber(projects.length)} proyectos</span>
          </div>
        </div>

        <div className="analytics-filters__grid analytics-filters__grid--extended">
          <label className="analytics-filter">
            <span><Search size={14} /> Buscar</span>
            <input type="search" value={filters.query} onChange={(e) => updateFilter('query', e.target.value)} placeholder="Nombre, código, dependencia…" />
          </label>

          <label className="analytics-filter">
            <span><Building2 size={14} /> Dependencia</span>
            <AutocompleteSelect
              value={filters.dependencia}
              onChange={(val) => updateFilter('dependencia', val)}
              options={dependencyOptions}
              placeholder="Buscar dependencia..."
              allLabel="Todas"
              allValue="ALL"
            />
          </label>

          <label className="analytics-filter">
            <span><Sparkles size={14} /> Clasificación</span>
            <AutocompleteSelect
              value={filters.peti}
              onChange={(val) => updateFilter('peti', val)}
              options={[
                { value: 'PETI', label: 'Solo PETI' },
                { value: 'NO_PETI', label: 'Solo NO PETI' },
              ]}
              placeholder="Filtrar por PETI..."
              allLabel="PETI y NO PETI"
              allValue="ALL"
            />
          </label>

          <label className="analytics-filter">
            <span><Activity size={14} /> Estado</span>
            <AutocompleteSelect
              value={filters.estado}
              onChange={(val) => updateFilter('estado', val)}
              options={statusOptions}
              placeholder="Buscar estado..."
              allLabel="Todos"
              allValue="ALL"
            />
          </label>

          <label className="analytics-filter">
            <span><Calendar size={14} /> Fecha inicio desde</span>
            <input type="date" value={filters.fechaInicio} onChange={(e) => updateFilter('fechaInicio', e.target.value)} />
          </label>

          <label className="analytics-filter">
            <span><Calendar size={14} /> Fecha fin hasta</span>
            <input type="date" value={filters.fechaFin} onChange={(e) => updateFilter('fechaFin', e.target.value)} />
          </label>

          <label className="analytics-filter">
            <span><SlidersHorizontal size={14} /> Ordenar por</span>
            <AutocompleteSelect
              value={filters.sortBy}
              onChange={(val) => updateFilter('sortBy', val)}
              options={SORT_OPTIONS}
              placeholder="Seleccionar orden..."
              sortAlphabetically={false}
            />
          </label>

          <div className="analytics-filter analytics-filter--summary">
            <span><Sparkles size={14} /> Resumen</span>
            <strong>{formatNumber(summary.total)} proyectos</strong>
            <small>{formatNumber(summary.petiProjects)} PETI · {formatNumber(summary.noPetiProjects)} NO PETI</small>
          </div>
        </div>

        {activeFilterChips.length > 0 ? (
          <div className="analytics-active-filters">
            {activeFilterChips.map((chip) => (
              <button key={chip.key} type="button" className="analytics-chip"
                onClick={() => {
                  if (chip.key === 'query') updateFilter('query', '');
                  if (chip.key === 'dependencia') updateFilter('dependencia', 'ALL');
                  if (chip.key === 'peti') updateFilter('peti', 'ALL');
                  if (chip.key === 'estado') updateFilter('estado', 'ALL');
                  if (chip.key === 'fechaInicio') updateFilter('fechaInicio', '');
                  if (chip.key === 'fechaFin') updateFilter('fechaFin', '');
                }}>
                {chip.label}<X size={13} />
              </button>
            ))}
          </div>
        ) : null}
      </section>

      {/* â”€â”€ KPI Cards â”€â”€ */}
      <section className="analytics-kpis analytics-kpis--wide">
        <article className="analytics-kpi-card analytics-kpi-card--accent">
          <span><Target size={14} /> Proyectos</span>
          <strong>{formatNumber(summary.total)}</strong>
          <small>{formatNumber(summary.enTiempo)} en tiempo · {formatNumber(summary.enAtraso)} en atraso</small>
          <div className="kpi-spark kpi-spark--blue" />
        </article>
        <article className="analytics-kpi-card">
          <span><Sparkles size={14} /> PETI / No PETI</span>
          <strong>{formatNumber(summary.petiProjects)} <small className="kpi-slash">/ {formatNumber(summary.noPetiProjects)}</small></strong>
          <small>Estratégicos vs operativos</small>
          <div className="kpi-spark kpi-spark--cyan" />
        </article>
        <article className="analytics-kpi-card">
          <span><BarChart3 size={14} /> Avance promedio</span>
          <strong>{formatPercent(summary.avancePromedio)}</strong>
          <small>Progreso físico del portafolio</small>
          <div className="kpi-progress" style={{ '--kpi-val': `${clampPercent(summary.avancePromedio)}%`, '--kpi-color': '#3b82f6' }} />
        </article>
        <article className="analytics-kpi-card">
          <span><Target size={14} /> Eficacia promedio</span>
          <strong className={toneClass(summary.eficaciaPromedio)}>{formatPercent(summary.eficaciaPromedio)}</strong>
          <small>Programado vs ejecutado</small>
          <div className="kpi-progress" style={{ '--kpi-val': `${clampPercent(summary.eficaciaPromedio)}%`, '--kpi-color': '#10b981' }} />
        </article>
        <article className="analytics-kpi-card">
          <span><Zap size={14} /> Eficiencia promedio</span>
          <strong className={toneClass(summary.eficienciaPromedio)}>{formatPercent(summary.eficienciaPromedio)}</strong>
          <small>Entregas a tiempo / total entregas</small>
          <div className="kpi-progress" style={{ '--kpi-val': `${clampPercent(summary.eficienciaPromedio)}%`, '--kpi-color': '#f59e0b' }} />
        </article>
        <article className="analytics-kpi-card">
          <span><TrendingUp size={14} /> Cobertura FURAG</span>
          <strong>{formatPercent(summary.furagPromedio)}</strong>
          <small>{visibleRiskText}</small>
          <div className="kpi-progress" style={{ '--kpi-val': `${clampPercent(summary.furagPromedio)}%`, '--kpi-color': '#8b5cf6' }} />
        </article>
        <article className="analytics-kpi-card">
          <span><ShieldCheck size={14} /> Mitigación riesgos</span>
          <strong>{formatPercent(summary.mitigacionPromedio)}</strong>
          <small>{formatNumber(data?.riesgos?.tratados)} tratados en total</small>
          <div className="kpi-progress" style={{ '--kpi-val': `${clampPercent(summary.mitigacionPromedio)}%`, '--kpi-color': '#ef4444' }} />
        </article>
        <article className="analytics-kpi-card analytics-kpi-card--health">
          <span><Activity size={14} /> Salud portafolio</span>
          <strong className={toneClass(summary.saludPortafolio)}>{formatPercent(summary.saludPortafolio)}</strong>
          <small>{formatNumber(summary.enTiempo)} proyectos en tiempo</small>
          <div className="kpi-progress" style={{ '--kpi-val': `${clampPercent(summary.saludPortafolio)}%`, '--kpi-color': summary.saludPortafolio >= 80 ? '#10b981' : summary.saludPortafolio >= 50 ? '#f59e0b' : '#ef4444' }} />
        </article>
      </section>
      <>
        <div className="analytics-charts-grid">
          <section className="analytics-panel analytics-panel--chart">
            <div className="analytics-panel__header">
              <div>
                <h2>Dispersión Eficiencia vs Eficacia</h2>
                <p>Cada punto es un proyecto - tamaño representa el avance físico.</p>
              </div>
              <div className="analytics-panel__meta">
                <span><Zap size={14} /> {formatNumber(scatterData.length)} proyectos</span>
              </div>
            </div>
            {scatterData.length === 0 ? (
              <div className="analytics-empty">No hay datos disponibles para los filtros actuales.</div>
            ) : (
              <div className="chart-container">
                <ResponsiveContainer width="100%" height={320}>
                  <ScatterChart margin={{ top: 10, right: 20, bottom: 20, left: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                    <XAxis dataKey="x" name="Eficiencia" unit="%" type="number" domain={[0, 100]} tick={axisStyle} label={{ value: 'Eficiencia (%)', position: 'insideBottom', offset: -12, style: axisStyle }} />
                    <YAxis dataKey="y" name="Eficacia" unit="%" type="number" domain={[0, 100]} tick={axisStyle} label={{ value: 'Eficacia (%)', angle: -90, position: 'insideLeft', offset: 8, style: axisStyle }} />
                    <ZAxis dataKey="z" range={[60, 400]} name="Avance" />
                    <Tooltip content={<ScatterTooltipContent />} cursor={{ strokeDasharray: '4 4' }} />
                    <Scatter data={scatterData} fill="#3b82f6" fillOpacity={0.75}>
                      {scatterData.map((_, i) => (
                        <Cell key={i} fill={CHART_COLORS_PASTEL[i % CHART_COLORS_PASTEL.length]} fillOpacity={0.8} />
                      ))}
                    </Scatter>
                  </ScatterChart>
                </ResponsiveContainer>
                <div className="chart-quadrant-legend">
                  <span className="chart-quadrant-legend__item chart-quadrant-legend__item--tl">Eficacia Alta / Eficiencia Baja</span>
                  <span className="chart-quadrant-legend__item chart-quadrant-legend__item--tr">Cuadrante Ideal</span>
                  <span className="chart-quadrant-legend__item chart-quadrant-legend__item--bl">Bajo rendimiento</span>
                  <span className="chart-quadrant-legend__item chart-quadrant-legend__item--br">Eficacia Baja / Eficiencia Alta</span>
                </div>
              </div>
            )}
          </section>

          <section className="analytics-panel analytics-panel--chart">
            <div className="analytics-panel__header">
              <div>
                <h2>Distribución de Estados</h2>
                <p>Composición del portafolio por estado actual.</p>
              </div>
            </div>
            {donutData.length === 0 ? (
              <div className="analytics-empty">No hay estados registrados.</div>
            ) : (
              <div className="chart-container">
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie data={donutData} cx="50%" cy="50%" innerRadius={70} outerRadius={115} paddingAngle={3} dataKey="value" labelLine={false} label={renderCustomLabel}>
                      {donutData.map((_, i) => (
                        <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} stroke="none" />
                      ))}
                    </Pie>
                    <Tooltip content={<DonutTooltipContent />} />
                  </PieChart>
                </ResponsiveContainer>
                <div className="chart-donut-legend">
                  {donutData.map((entry, i) => (
                    <div key={entry.name} className="chart-donut-legend__item">
                      <span className="chart-donut-legend__dot" style={{ background: CHART_COLORS[i % CHART_COLORS.length] }} />
                      <span className="chart-donut-legend__name">{entry.name}</span>
                      <strong>{entry.value}</strong>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        </div>

        <div className="analytics-charts-grid">
          <section className="analytics-panel analytics-panel--chart">
            <div className="analytics-panel__header">
              <div>
                <h2>Rendimiento por Dependencia</h2>
                <p>Avance, Eficacia y Eficiencia promedio por área organizacional.</p>
              </div>
              <div className="analytics-panel__meta">
                <span><Building2 size={14} /> Top {barByDependencyData.length} dependencias</span>
              </div>
            </div>
            {barByDependencyData.length === 0 ? (
              <div className="analytics-empty">Sin dependencias para el filtro activo.</div>
            ) : (
              <div className="chart-container">
                <ResponsiveContainer width="100%" height={320}>
                  <BarChart data={barByDependencyData} margin={{ top: 10, right: 10, left: 0, bottom: 50 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                    <XAxis dataKey="name" tick={{ ...axisStyle, fontSize: 10 }} angle={-35} textAnchor="end" interval={0} />
                    <YAxis unit="%" domain={[0, 100]} tick={axisStyle} />
                    <Tooltip content={<BarTooltipContent />} cursor={{ fill: 'var(--border-color)', opacity: 0.3 }} />
                    <Legend wrapperStyle={{ paddingTop: '8px', fontSize: '12px' }} />
                    <Bar dataKey="Avance" fill="#3b82f6" radius={[4, 4, 0, 0]} maxBarSize={20} />
                    <Bar dataKey="Eficacia" fill="#10b981" radius={[4, 4, 0, 0]} maxBarSize={20} />
                    <Bar dataKey="Eficiencia" fill="#f59e0b" radius={[4, 4, 0, 0]} maxBarSize={20} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            )}
          </section>

          <section className="analytics-panel analytics-panel--chart">
            <div className="analytics-panel__header">
              <div>
                <h2>Radiografía del Portafolio</h2>
                <p>Equilibrio de los 5 indicadores clave del portafolio filtrado.</p>
              </div>
            </div>
            {radarData.length === 0 ? (
              <div className="analytics-empty">Sin datos para el filtro activo.</div>
            ) : (
              <div className="chart-container chart-container--centered">
                <ResponsiveContainer width="100%" height={320}>
                  <RadarChart data={radarData} margin={{ top: 20, right: 30, bottom: 20, left: 30 }}>
                    <PolarGrid stroke="var(--border-color)" />
                    <PolarAngleAxis dataKey="metric" tick={{ ...axisStyle, fontSize: 12, fontWeight: 700 }} />
                    <PolarRadiusAxis angle={90} domain={[0, 100]} tick={{ ...axisStyle, fontSize: 9 }} tickCount={5} />
                    <Radar name="Portafolio" dataKey="Portafolio" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.25} strokeWidth={2} dot={{ r: 4, fill: '#3b82f6' }} />
                    <Tooltip formatter={(v) => `${v}%`} />
                    <Legend />
                  </RadarChart>
                </ResponsiveContainer>
                <div className="radar-kpi-row">
                  {radarData.map((r) => (
                    <div key={r.metric} className="radar-kpi">
                      <span>{r.metric}</span>
                      <strong className={toneClass(r.Portafolio)}>{r.Portafolio}%</strong>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </section>
        </div>

        <section className="analytics-panel">
          <div className="analytics-panel__header">
            <div>
              <h2>Comparativa entre proyectos</h2>
              <p>Ranking de los top 10 proyectos con el filtro activo - Avance · Eficacia · Eficiencia.</p>
            </div>
            <div className="analytics-panel__meta">
              <span><TrendingUp size={14} /> {formatNumber(summary.enTiempo)} en tiempo</span>
              <span><TrendingDown size={14} /> {formatNumber(summary.enAtraso)} en atraso</span>
            </div>
          </div>
          <div className="project-bars">
            {comparisonProjects.length === 0 ? (
              <div className="analytics-empty">No hay proyectos para el filtro activo.</div>
            ) : (
              <>
                <div className="project-bars__list">
                  {comparisonProjectsPage.map((project, index) => {
                    const isDelayed = normalizeText(project?.estado).includes('atras');
                    const statusClass = isDelayed ? 'analytics-project-tag analytics-project-tag--danger' : 'analytics-project-tag analytics-project-tag--success';
                    return (
                      <div key={project.proyectoId} className="project-bars__row">
                        <div className="project-bars__icon"><Building2 size={18} /></div>
                        <div className="project-bars__meta">
                          <strong>{project.nombre}</strong>
                          <span>{project.dependencia}</span>
                          <div className="project-bars__tags">
                            <span className={`analytics-project-tag ${isPeti(project) ? 'analytics-project-tag--primary' : 'analytics-project-tag--soft'}`}>{isPeti(project) ? 'PETI' : 'NO PETI'}</span>
                            <span className={statusClass}>{project.estado ?? 'Sin estado'}</span>
                          </div>
                        </div>
                        <div className="project-bars__metric">
                          <div className="project-bars__metric-head"><span>Avance</span><strong>{formatPercent(project.avance)}</strong></div>
                          <div className="project-bars__track"><span style={{ width: `${clampPercent(project.avance)}%`, background: CHART_COLORS[index % CHART_COLORS.length] }} /></div>
                        </div>
                        <div className="project-bars__metric">
                          <div className="project-bars__metric-head"><span>Eficacia</span><strong className={toneClass(project.eficacia)}>{formatPercent(project.eficacia)}</strong></div>
                          <div className="project-bars__track project-bars__track--secondary"><span style={{ width: `${clampPercent(project.eficacia)}%`, background: 'linear-gradient(90deg, var(--success), color-mix(in srgb, var(--success) 55%, var(--primary)))' }} /></div>
                        </div>
                        <div className="project-bars__metric">
                          <div className="project-bars__metric-head"><span>Eficiencia</span><strong className={toneClass(project.eficiencia)}>{formatPercent(project.eficiencia)}</strong></div>
                          <div className="project-bars__track project-bars__track--tertiary"><span style={{ width: `${clampPercent(project.eficiencia)}%`, background: 'linear-gradient(90deg, var(--warning), color-mix(in srgb, var(--warning) 55%, var(--primary)))' }} /></div>
                        </div>
                        <button type="button" className="project-bars__chevron" aria-label={`Ver ${project.nombre}`}>
                          <ChevronRight size={18} />
                        </button>
                      </div>
                    );
                  })}
                </div>
                <div className="analytics-pagination">
                  <span className="analytics-pagination__summary">Mostrando {Math.min((comparisonPageSafe - 1) * COMPARISON_PAGE_SIZE + 1, comparisonProjects.length)} a {Math.min(comparisonPageSafe * COMPARISON_PAGE_SIZE, comparisonProjects.length)} de {comparisonProjects.length} proyectos</span>
                  <div className="analytics-pagination__controls">
                    <button type="button" className="analytics-pagination__button" onClick={() => setComparisonPage((current) => Math.max(1, current - 1))} disabled={comparisonPageSafe <= 1}>‹</button>
                    {Array.from({ length: comparisonTotalPages }, (_, index) => index + 1).map((page) => (
                      <button key={page} type="button" className={`analytics-pagination__button ${page === comparisonPageSafe ? 'is-active' : ''}`} onClick={() => setComparisonPage(page)}>{page}</button>
                    ))}
                    <button type="button" className="analytics-pagination__button" onClick={() => setComparisonPage((current) => Math.min(comparisonTotalPages, current + 1))} disabled={comparisonPageSafe >= comparisonTotalPages}>›</button>
                  </div>
                </div>
              </>
            )}
          </div>
        </section>
      </>

      {loading ? <div className="analytics-loading">Cargando analíticas...</div> : null}
    </div>
  );
};

export default AnalyticsPage;
