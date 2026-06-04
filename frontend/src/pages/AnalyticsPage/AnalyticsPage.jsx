import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  BarChart3,
  Building2,
  Calendar,
  Filter,
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
import './AnalyticsPage.css';

/* ─────────────────── Constants ─────────────────── */
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

/* ─────────────────── Helpers ─────────────────── */
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

/* ─────────────────── Custom Tooltip Helpers ─────────────────── */
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

/* ─────────────────── Custom Donut Label ─────────────────── */
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

/* ─────────────────── Main Component ─────────────────── */
const AnalyticsPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [activeTab, setActiveTab] = useState('charts'); // 'charts' | 'matrix'

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

  useEffect(() => {
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

  /* ─── Chart Data ─── */
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
      Mitigación: Math.round(summary.mitigacionPromedio),
    };
    return [
      { metric: 'Avance', Portafolio: portAvg.Avance },
      { metric: 'Eficacia', Portafolio: portAvg.Eficacia },
      { metric: 'Eficiencia', Portafolio: portAvg.Eficiencia },
      { metric: 'FURAG', Portafolio: portAvg.FURAG },
      { metric: 'Mitigación', Portafolio: portAvg.Mitigación },
    ];
  }, [filteredProjects, summary]);

  const dependencyBreakdown = useMemo(() => {
    const grouped = new Map();
    filteredProjects.forEach((project) => {
      const key = project?.dependencia?.trim() || 'Sin dependencia';
      const normalizedKey = normalizeText(key);
      const current = grouped.get(normalizedKey) ?? { key: normalizedKey, dependencia: key, totalProyectos: 0, avanceSum: 0, eficaciaSum: 0, eficienciaSum: 0, furagSum: 0, mitigacionSum: 0 };
      current.totalProyectos += 1;
      current.avanceSum += clampPercent(project?.avance);
      current.eficaciaSum += clampPercent(project?.eficacia);
      current.eficienciaSum += clampPercent(project?.eficiencia);
      current.furagSum += clampPercent(getFuragCoverage(project));
      current.mitigacionSum += clampPercent(project?.indiceMitigacion);
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
      .sort((a, b) => b.avancePromedio - a.avancePromedio);
  }, [filteredProjects, getFuragCoverage]);

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

  /* ─── Recharts shared axis tick style ─── */
  const axisStyle = { fontSize: 11, fill: 'var(--text-muted)' };

  return (
    <div className="analytics-page">
      {/* ── Hero ── */}
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
          <button className="analytics-refresh analytics-refresh--secondary" onClick={resetFilters}>
            <X size={18} /> Limpiar filtros
          </button>
        </div>
      </section>

      {/* ── Error ── */}
      {error ? (
        <div className="analytics-alert">
          <AlertTriangle size={18} /><span>{error}</span>
        </div>
      ) : null}

      {/* ── Filters ── */}
      <section className="analytics-panel analytics-filters">
        <div className="analytics-panel__header analytics-panel__header--filters">
          <div>
            <h2>Filtros interactivos</h2>
            <p>Slicers que cruzan todos los visuales al estilo de un tablero ejecutivo.</p>
          </div>
          <div className="analytics-panel__meta">
            <span><Filter size={14} /> {activeFilterChips.length} activos</span>
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
            <select value={filters.dependencia} onChange={(e) => updateFilter('dependencia', e.target.value)}>
              <option value="ALL">Todas</option>
              {dependencyOptions.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          </label>

          <label className="analytics-filter">
            <span><Sparkles size={14} /> Clasificación</span>
            <select value={filters.peti} onChange={(e) => updateFilter('peti', e.target.value)}>
              <option value="ALL">PETI y NO PETI</option>
              <option value="PETI">Solo PETI</option>
              <option value="NO_PETI">Solo NO PETI</option>
            </select>
          </label>

          <label className="analytics-filter">
            <span><Activity size={14} /> Estado</span>
            <select value={filters.estado} onChange={(e) => updateFilter('estado', e.target.value)}>
              <option value="ALL">Todos</option>
              {statusOptions.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
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
            <select value={filters.sortBy} onChange={(e) => updateFilter('sortBy', e.target.value)}>
              {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
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

      {/* ── KPI Cards ── */}
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

      {/* ── Tab Toggle ── */}
      <div className="analytics-tab-bar">
        <button className={`analytics-tab ${activeTab === 'charts' ? 'analytics-tab--active' : ''}`} onClick={() => setActiveTab('charts')}>
          <BarChart3 size={16} /> Gráficos
        </button>
        <button className={`analytics-tab ${activeTab === 'matrix' ? 'analytics-tab--active' : ''}`} onClick={() => setActiveTab('matrix')}>
          <Filter size={16} /> Matriz de datos
        </button>
      </div>

      {activeTab === 'charts' && (
        <>
          {/* ── Charts row 1: Scatter + Donut ── */}
          <div className="analytics-charts-grid">
            {/* Scatter: Eficiencia vs Eficacia */}
            <section className="analytics-panel analytics-panel--chart">
              <div className="analytics-panel__header">
                <div>
                  <h2>Matriz Eficiencia vs Eficacia</h2>
                  <p>Cada punto es un proyecto — tamaño representa el avance físico.</p>
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
                      {/* Cuadrante ideal reference lines rendered via custom SVG labels - simplified */}
                    </ScatterChart>
                  </ResponsiveContainer>
                  <div className="chart-quadrant-legend">
                    <span className="chart-quadrant-legend__item chart-quadrant-legend__item--tl">Eficacia Alta / Eficiencia Baja</span>
                    <span className="chart-quadrant-legend__item chart-quadrant-legend__item--tr">✦ Cuadrante Ideal</span>
                    <span className="chart-quadrant-legend__item chart-quadrant-legend__item--bl">Bajo rendimiento</span>
                    <span className="chart-quadrant-legend__item chart-quadrant-legend__item--br">Eficacia Baja / Eficiencia Alta</span>
                  </div>
                </div>
              )}
            </section>

            {/* Donut: Distribución de estados */}
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
                      <Pie
                        data={donutData}
                        cx="50%"
                        cy="50%"
                        innerRadius={70}
                        outerRadius={115}
                        paddingAngle={3}
                        dataKey="value"
                        labelLine={false}
                        label={renderCustomLabel}
                      >
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

          {/* ── Charts row 2: Grouped Bar + Radar ── */}
          <div className="analytics-charts-grid">
            {/* Grouped Bar: por dependencia */}
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

            {/* Radar: Radiografía del portafolio */}
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

          {/* ── Comparison bars (top 10) ── */}
          <section className="analytics-panel">
            <div className="analytics-panel__header">
              <div>
                <h2>Comparativa entre proyectos</h2>
                <p>Ranking de los top 10 proyectos con el filtro activo — Avance · Eficacia · Eficiencia.</p>
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
                comparisonProjects.map((project, index) => (
                  <div key={project.proyectoId} className="project-bars__row">
                    <div className="project-bars__meta">
                      <strong>{project.nombre}</strong>
                      <span>{project.dependencia}</span>
                      <small>{isPeti(project) ? 'PETI' : 'NO PETI'} · {project.estado ?? 'Sin estado'}</small>
                    </div>
                    <div className="project-bars__chart">
                      <div className="project-bars__track">
                        <span style={{ width: `${clampPercent(project.avance)}%`, background: CHART_COLORS[index % CHART_COLORS.length] }} />
                      </div>
                      <div className="project-bars__track project-bars__track--secondary">
                        <span style={{ width: `${clampPercent(project.eficacia)}%`, background: 'linear-gradient(90deg, var(--success), color-mix(in srgb, var(--success) 55%, var(--primary)))' }} />
                      </div>
                      <div className="project-bars__track project-bars__track--tertiary">
                        <span style={{ width: `${clampPercent(project.eficiencia)}%`, background: 'linear-gradient(90deg, var(--warning), color-mix(in srgb, var(--warning) 55%, var(--primary)))' }} />
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
        </>
      )}

      {activeTab === 'matrix' && (
        <>
          {/* ── Dependency breakdown ── */}
          <section className="analytics-grid analytics-grid--powerbi">
            <article className="analytics-card analytics-card--wide">
              <div className="analytics-card__title">
                <h3>Distribución por dependencia</h3>
                <span>{formatNumber(dependencyBreakdown.length)} dependencias</span>
              </div>
              <div className="analytics-bars">
                {dependencyBreakdown.length === 0 ? (
                  <div className="analytics-empty">Sin datos para el filtro activo.</div>
                ) : (
                  dependencyBreakdown.map((item, index) => (
                    <button key={item.key} type="button" className="analytics-bar analytics-bar--button"
                      onClick={() => updateFilter('dependencia', item.dependencia)}>
                      <div className="analytics-bar__head">
                        <strong>{item.dependencia}</strong>
                        <span>{formatPercent(item.avancePromedio)}</span>
                      </div>
                      <div className="analytics-bar__track">
                        <span style={{ width: `${clampPercent(item.avancePromedio)}%`, background: `linear-gradient(90deg, ${CHART_COLORS[index % CHART_COLORS.length]}, color-mix(in srgb, ${CHART_COLORS[index % CHART_COLORS.length]} 65%, var(--success)))` }} />
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
                <span>PETI vs No PETI</span>
              </div>
              <div className="strategy-stack">
                <div className="strategy-stack__rail">
                  {summary.petiProjects > 0 && (
                    <button type="button" className="strategy-stack__segment strategy-stack__segment--button"
                      style={{ flex: `${summary.petiProjects} 1 0%`, background: '#3b82f6' }}
                      onClick={() => updateFilter('peti', 'PETI')}>
                      <span>PETI</span>
                    </button>
                  )}
                  {summary.noPetiProjects > 0 && (
                    <button type="button" className="strategy-stack__segment strategy-stack__segment--button"
                      style={{ flex: `${summary.noPetiProjects} 1 0%`, background: '#10b981' }}
                      onClick={() => updateFilter('peti', 'NO_PETI')}>
                      <span>NO PETI</span>
                    </button>
                  )}
                </div>
                <div className="strategy-stack__legend">
                  <button type="button" className="strategy-stack__legend-item strategy-stack__legend-item--button" onClick={() => updateFilter('peti', 'PETI')}>
                    <span style={{ background: '#3b82f6' }} />
                    <strong>PETI</strong>
                    <small>{formatNumber(summary.petiProjects)} proyectos estratégicos · {formatPercent(average(filteredProjects.filter(isPeti), (p) => p?.avance))} avance</small>
                  </button>
                  <button type="button" className="strategy-stack__legend-item strategy-stack__legend-item--button" onClick={() => updateFilter('peti', 'NO_PETI')}>
                    <span style={{ background: '#10b981' }} />
                    <strong>NO PETI</strong>
                    <small>{formatNumber(summary.noPetiProjects)} iniciativas operativas · {formatPercent(average(filteredProjects.filter((p) => !isPeti(p)), (p) => p?.avance))} avance</small>
                  </button>
                </div>
              </div>
            </article>
          </section>

          {/* ── Full data matrix ── */}
          <section className="analytics-panel">
            <div className="analytics-panel__header">
              <div>
                <h2>Matriz comparativa</h2>
                <p>Lectura tabular completa con todas las métricas por proyecto.</p>
              </div>
              <div className="analytics-panel__meta">
                <span><BarChart3 size={14} /> {formatNumber(filteredProjects.length)} filas</span>
                <span><ShieldCheck size={14} /> {formatPercent(summary.mitigacionPromedio)} mitigación prom.</span>
              </div>
            </div>
            <div className="analytics-matrix">
              {filteredProjectsSorted.length === 0 ? (
                <div className="analytics-empty">No hay filas para el filtro activo.</div>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>Proyecto</th>
                      <th>Dependencia</th>
                      <th>Clasif.</th>
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
                          <td><strong>{project.nombre}</strong><small>{project.proyectoId}</small></td>
                          <td>{project.dependencia}</td>
                          <td>
                            <span className={isPeti(project) ? 'analytics-pill analytics-pill--primary' : 'analytics-pill'}>
                              {isPeti(project) ? 'PETI' : 'NO PETI'}
                            </span>
                          </td>
                          <td>
                            <div className="analytics-cell-metric">
                              <strong>{formatPercent(project.avance)}</strong>
                              <div className="analytics-cell-track"><span style={{ width: `${clampPercent(project.avance)}%` }} /></div>
                            </div>
                          </td>
                          <td>
                            <div className="analytics-cell-metric">
                              <strong>{formatPercent(project.eficacia)}</strong>
                              <div className="analytics-cell-track"><span style={{ width: `${clampPercent(project.eficacia)}%`, background: 'linear-gradient(90deg, var(--success), color-mix(in srgb, var(--success) 55%, var(--primary)))' }} /></div>
                            </div>
                          </td>
                          <td>
                            <div className="analytics-cell-metric">
                              <strong>{formatPercent(project.eficiencia)}</strong>
                              <div className="analytics-cell-track"><span style={{ width: `${clampPercent(project.eficiencia)}%`, background: 'linear-gradient(90deg, var(--warning), color-mix(in srgb, var(--warning) 55%, var(--primary)))' }} /></div>
                            </div>
                          </td>
                          <td>
                            <div className="analytics-cell-metric">
                              <strong>{formatPercent(furagCoverage)}</strong>
                              <div className="analytics-cell-track"><span style={{ width: `${clampPercent(furagCoverage)}%`, background: 'linear-gradient(90deg, #8b5cf6, color-mix(in srgb, #8b5cf6 55%, var(--primary)))' }} /></div>
                            </div>
                          </td>
                          <td>
                            <div className="analytics-cell-metric">
                              <strong>{formatPercent(project.indiceMitigacion)}</strong>
                              <div className="analytics-cell-track"><span style={{ width: `${clampPercent(project.indiceMitigacion)}%`, background: 'linear-gradient(90deg, #22c55e, color-mix(in srgb, #22c55e 55%, var(--primary)))' }} /></div>
                            </div>
                          </td>
                          <td><span className={tone}>{project.estado ?? 'Sin estado'}</span></td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </section>
        </>
      )}

      {loading ? <div className="analytics-loading">Cargando analíticas…</div> : null}
    </div>
  );
};

export default AnalyticsPage;
