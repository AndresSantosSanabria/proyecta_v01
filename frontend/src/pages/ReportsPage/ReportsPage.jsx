import { useEffect, useMemo, useState } from 'react';
import {
  BookText,
  ChevronRight,
  ClipboardList,
  Download,
  Eye,
  FileBarChart2,
  FileSearch,
  FileSpreadsheet,
  FileText,
  Radar,
  Search,
  ShieldAlert,
  TimerReset,
  X,
} from 'lucide-react';
import projectService from '../../services/projectService';
import reportService from '../../services/reportService';
import './ReportsPage.css';

const REPORT_BEHAVIORS = {
  ESTADO_PROYECTO: {
    title: 'Reporte 1 - Estado de Proyecto Específico',
    requiresProject: true,
    downloadLabel: 'Descargar PDF',
    previewIcon: FileSearch,
    kind: 'project',
    filename: (projectId) => `reporte-estado-proyecto-especifico-${projectId}.pdf`,
    download: (projectId) => reportService.downloadProjectPdf(projectId),
    fetchData: (projectId) => reportService.getProjectPreview(projectId),
  },
  TODOS_LOS_PROYECTOS: {
    title: 'Reporte 2 - Estado de Todos los Proyectos',
    requiresProject: false,
    downloadLabel: 'Descargar PDF',
    previewIcon: FileBarChart2,
    kind: 'portfolio',
    filename: () => 'reporte-estado-todos-los-proyectos.pdf',
    download: () => reportService.downloadPortafolioPdf(),
    downloadExcel: () => reportService.downloadPortafolioExcel(),
    fetchData: () => reportService.getAllProjectsSummary(),
  },
  RETRASOS_ENTREGA: {
    title: 'Reporte 3 - Proyectos con Retrasos en la Fecha de Entrega',
    requiresProject: false,
    downloadLabel: 'Descargar PDF',
    previewIcon: TimerReset,
    kind: 'delays',
    filename: () => 'reporte-proyectos-con-retrasos-en-la-fecha-de-entrega.pdf',
    download: () => reportService.downloadDelayedProjectsPdf(),
    fetchData: () => reportService.getDelayedProjects(),
  },
  PLAN_COMUNICACIONES: {
    title: 'Reporte 4 - Plan de Comunicaciones',
    requiresProject: false,
    downloadLabel: 'Descargar PDF',
    previewIcon: BookText,
    kind: 'plan',
    filename: () => 'reporte-plan-comunicaciones.pdf',
    download: () => reportService.downloadPlanComunicacionesPdf(),
    fetchData: async () => ({}),
  },
  FURAG: {
    title: 'Reporte 5 - Preguntas FURAG',
    requiresProject: true,
    downloadLabel: 'Descargar PDF',
    previewIcon: ClipboardList,
    kind: 'furag',
    filename: (projectId) => `reporte-furag-${projectId}.pdf`,
    download: (projectId) => reportService.downloadFuragPdf(projectId),
    fetchData: (projectId) => reportService.getFurag(projectId),
  },
  VERIFICACION_RIESGOS: {
    title: 'Reporte 6 - Verificación de Tratamiento a Riesgos',
    requiresProject: false,
    downloadLabel: 'Descargar PDF',
    previewIcon: Radar,
    kind: 'risks',
    filename: () => 'reporte-verificacion-tratamiento-a-riesgos.pdf',
    download: () => reportService.downloadRiesgosPdf(),
    fetchData: () => reportService.getRiesgosVerificacion(),
  },
};

const normalizeText = (value) =>
  String(value || '')
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
    .trim();

const sameText = (left, right) => normalizeText(left) === normalizeText(right);

const getFuragAnswers = (project) => project?.furag?.respuestas || project?.furag || {};

const hasText = (value) => value !== null && value !== undefined && String(value).trim() !== '';

const furagCompleto = (project) => {
  const furag = getFuragAnswers(project);
  return Object.values(furag).every((value) => hasText(value));
};

const FALLBACK_REPORTS = [
  {
    id: 'ESTADO_PROYECTO',
    nombre: 'Estado de Proyecto Específico',
    descripcion: 'Vista ejecutiva del avance, entregables vencidos y estado del proyecto',
  },
  {
    id: 'TODOS_LOS_PROYECTOS',
    nombre: 'Estado de Todos los Proyectos',
    descripcion: 'Resumen consolidado con KPIs del portafolio activo',
  },
  {
    id: 'RETRASOS_ENTREGA',
    nombre: 'Proyectos con Retrasos en la Fecha de Entrega',
    descripcion: 'Listado ejecutivo de proyectos con entregables fuera de plazo',
  },
  {
    id: 'PLAN_COMUNICACIONES',
    nombre: 'Plan de Comunicaciones',
    descripcion: 'Detalle del plan de comunicaciones asociado a un proyecto',
  },
  {
    id: 'FURAG',
    nombre: 'Preguntas FURAG',
    descripcion: 'Consolidado institucional de respuestas FURAG por dependencia',
  },
  {
    id: 'VERIFICACION_RIESGOS',
    nombre: 'Verificación de Tratamiento a Riesgos',
    descripcion: 'Reporte de riesgos y su estado de tratamiento',
  },
];

const unwrap = (payload) => payload?.data?.data ?? payload?.data ?? payload ?? null;

const toPercent = (value) => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '0.00%';
  }
  return `${Number(value).toFixed(2)}%`;
};

const ReportsPage = () => {
  const [reportConfigs, setReportConfigs] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [selectedReportId, setSelectedReportId] = useState('ESTADO_PROYECTO');
  const [reportQuery, setReportQuery] = useState('');
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [projectQuery, setProjectQuery] = useState('');
  const [detailMode, setDetailMode] = useState('resumido');
  const [reportData, setReportData] = useState(null);
  const [loadingReport, setLoadingReport] = useState(false);
  const [reportError, setReportError] = useState('');
  const [previewModal, setPreviewModal] = useState({ open: false, title: '', url: '' });
  const [downloadBusy, setDownloadBusy] = useState(false);
  const [reportActionBusy, setReportActionBusy] = useState(false);
  
  const availableReports = reportConfigs.length > 0 ? reportConfigs : FALLBACK_REPORTS;
  const filteredReports = useMemo(() => {
    const query = normalizeText(reportQuery);
    if (!query) {
      return availableReports;
    }

    return availableReports.filter((report) =>
      [report.id, report.nombre, report.descripcion]
        .filter(Boolean)
        .some((value) => normalizeText(value).includes(query))
    );
  }, [availableReports, reportQuery]);

  const currentDateLabel = useMemo(
    () =>
      new Intl.DateTimeFormat('es-CO', {
        day: '2-digit',
        month: 'long',
        year: 'numeric',
      }).format(new Date()),
    []
  );

  const selectedReport = useMemo(
    () => availableReports.find((report) => report.id === selectedReportId) || null,
    [availableReports, selectedReportId]
  );

  const selectedBehavior = REPORT_BEHAVIORS[selectedReportId];

  const effectiveProjectId = useMemo(() => {
    if (!selectedBehavior?.requiresProject) {
      return '';
    }
    return selectedProjectId || projects[0]?.id || '';
  }, [selectedBehavior, selectedProjectId, projects]);

  const selectedProject = useMemo(
    () => projects.find((project) => project.id === effectiveProjectId) || null,
    [projects, effectiveProjectId]
  );

  const filteredProjects = useMemo(() => {
    const query = projectQuery.trim().toLowerCase();
    const furagDependency = selectedBehavior?.kind === 'furag'
      ? selectedProject?.dependencia || reportData?.dependencia || ''
      : '';

    const source = projects.filter((project) => {
      const matchesQuery = !query
        || [project.id, project.nombre, project.dependencia, project.estado]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(query));

      const matchesFurag = selectedBehavior?.kind !== 'furag'
        || (
          sameText(project?.dependencia, furagDependency)
          && furagCompleto(project)
        );

      return matchesQuery && matchesFurag;
    });

    return source.slice(0, 8);
  }, [projects, projectQuery, selectedBehavior, selectedProject, reportData]);

  useEffect(() => {
    let mounted = true;
    const loadConfigs = async () => {
      try {
        const response = await reportService.getConfigs();
        const configs = unwrap(response);
        if (mounted) {
          setReportConfigs(Array.isArray(configs) && configs.length > 0 ? configs : FALLBACK_REPORTS);
        }
      } catch {
        if (mounted) {
          setReportConfigs(FALLBACK_REPORTS);
        }
      }
    };

    loadConfigs();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    let mounted = true;
    const loadProjects = async () => {
      try {
        setLoadingProjects(true);
        const data = await projectService.getAllUnpaged({ size: 1000 });
        if (mounted) {
          setProjects(Array.isArray(data) ? data : []);
        }
      } catch {
        if (mounted) {
          setProjects([]);
        }
      } finally {
        if (mounted) {
          setLoadingProjects(false);
        }
      }
    };

    loadProjects();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    let mounted = true;
    const loadReportData = async () => {
      if (!selectedBehavior) {
        return;
      }
      if (selectedBehavior.requiresProject && !effectiveProjectId) {
        setReportData(null);
        return;
      }

      setLoadingReport(true);
      setReportError('');
      setReportData(null);

      try {
        const data = selectedBehavior.requiresProject
          ? await selectedBehavior.fetchData(effectiveProjectId)
          : await selectedBehavior.fetchData();
        if (mounted) {
          setReportData(unwrap(data));
        }
      } catch (error) {
        if (mounted) {
          setReportData(null);
          setReportError(error?.response?.data?.detail || 'No fue posible cargar la información del reporte.');
        }
      } finally {
        if (mounted) {
          setLoadingReport(false);
        }
      }
    };

    loadReportData();
    return () => {
      mounted = false;
    };
  }, [selectedReportId, effectiveProjectId, selectedBehavior]);

  useEffect(() => {
    if (!previewModal.open) {
      return undefined;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setPreviewModal((current) => {
          if (current.url) {
            window.URL.revokeObjectURL(current.url);
          }
          return { open: false, title: '', url: '' };
        });
      }
    };

    window.addEventListener('keydown', handleEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleEscape);
    };
  }, [previewModal.open]);

  useEffect(() => {
    return () => {
      if (previewModal.url) {
        window.URL.revokeObjectURL(previewModal.url);
      }
    };
  }, [previewModal.url]);

  const selectedBehaviorDownload = useMemo(() => {
    if (!selectedBehavior) {
      return null;
    }
    if (selectedBehavior.requiresProject && !effectiveProjectId) {
      return null;
    }
    return () =>
      selectedBehavior.requiresProject
        ? selectedBehavior.download(effectiveProjectId, detailMode)
        : selectedBehavior.download(detailMode);
  }, [detailMode, effectiveProjectId, selectedBehavior]);

  const reportContext = useMemo(() => {
    if (!selectedReport || !selectedBehavior) {
      return null;
    }

    const isProjectReport = Boolean(selectedBehavior.requiresProject);
    const statusLabel = loadingReport
      ? 'Actualizando'
      : reportError
        ? 'Con incidencia'
        : reportData
          ? 'Listo'
          : isProjectReport
            ? 'Seleccione un proyecto'
            : 'Pendiente';

    const filterLabelByKind = {
      project: 'Proyecto seleccionado',
      portfolio: 'Portafolio activo',
      delays: 'Proyectos con retrasos',
      plan: 'Proyectos No PETI',
      furag: 'Proyectos por dependencia',
      risks: 'Proyectos con cierre',
    };

    return {
      scopeLabel: isProjectReport ? 'Requiere proyecto' : 'Portafolio',
      statusLabel,
      filterLabel: filterLabelByKind[selectedBehavior.kind] || 'Filtro institucional',
      detailModeLabel: detailMode === 'detallado' ? 'Vista detallada' : 'Vista resumida',
      projectLabel: isProjectReport
        ? selectedProject
          ? `${selectedProject.id} · ${selectedProject.nombre}`
          : 'Sin proyecto seleccionado'
        : 'No aplica',
      detailLabel: isProjectReport
        ? selectedProject?.dependencia || 'Sin dependencia'
        : selectedBehavior.kind === 'risks'
          ? 'Tratamiento de riesgos'
          : selectedBehavior.kind === 'furag'
            ? 'Respuesta por dependencia'
            : 'Consulta institucional',
    };
  }, [detailMode, loadingReport, reportData, reportError, selectedBehavior, selectedProject, selectedReport]);

  const reportCards = useMemo(() => {
    if (!selectedBehavior || !reportData) {
      return [];
    }

    if (selectedBehavior.kind === 'project') {
      return [];
    }

    if (selectedBehavior.kind === 'portfolio') {
      const portfolioRows = Array.isArray(reportData) ? reportData : [];
      const total = portfolioRows.length;
      const atrasados = portfolioRows.reduce((sum, item) => sum + Number(item.entregablesAtrasados || 0), 0);
      const promedio = portfolioRows.length > 0
        ? portfolioRows.reduce((sum, item) => sum + Number(item.avance || 0), 0) / portfolioRows.length
        : 0;
      return [
        { label: 'Proyectos', value: total, tone: 'primary' },
        { label: 'Avance promedio', value: toPercent(promedio), tone: 'success' },
        { label: 'Entregables atrasados', value: atrasados, tone: 'danger' },
        { label: 'Dependencias', value: new Set(portfolioRows.map((item) => item.dependencia)).size, tone: 'neutral' },
      ];
    }

    if (selectedBehavior.kind === 'delays') {
      const delayRows = Array.isArray(reportData) ? reportData : [];
      const total = delayRows.length;
      const totalAtrasos = delayRows.reduce((sum, item) => sum + Number(item.entregablesAtrasados || 0), 0);
      
      const depCount = {};
      delayRows.forEach((item) => {
        const dep = item.dependencia || 'Sin dependencia';
        depCount[dep] = (depCount[dep] || 0) + 1;
      });
      const topDeps = Object.entries(depCount)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([dep, count]) => ({ dependencia: dep, count }));

      return [
        { label: 'Proyectos con retrasos', value: total, tone: 'danger' },
        { label: 'Atrasos acumulados', value: totalAtrasos, tone: 'warning' },
        { label: 'Top Dependencias', value: 'top3', tone: 'neutral', topDeps },
        { label: 'Proyectos revisados', value: projects.length, tone: 'primary' },
      ];
    }

    if (selectedBehavior.kind === 'plan') {
      const noPetiProjects = projects.filter((project) => !project?.peti);
      const withPlan = noPetiProjects.filter((project) => project?.planComunicacionesPdf).length;
      return [
        { label: 'Proyectos No PETI', value: noPetiProjects.length, tone: 'primary' },
        { label: 'Con plan', value: withPlan, tone: 'success' },
        { label: 'Sin plan', value: Math.max(noPetiProjects.length - withPlan, 0), tone: 'warning' },
        { label: 'Filtro', value: 'Proyectos No PETI', tone: 'neutral' },
      ];
    }

    if (selectedBehavior.kind === 'furag') {
      const dependency = selectedProject?.dependencia || reportData?.dependencia || '';
      const furagProjects = projects.filter((project) => sameText(project?.dependencia, dependency));
      const compliantProjects = furagProjects.filter((project) => furagCompleto(project));
      return [
        { label: 'Dependencia', value: dependency || 'No disponible', tone: 'neutral' },
        { label: 'Proyectos visibles', value: compliantProjects.length, tone: 'primary' },
        { label: 'Cumplen FURAG', value: compliantProjects.length, tone: 'success' },
        { label: 'Total revisados', value: furagProjects.length, tone: 'warning' },
      ];
    }

    if (selectedBehavior.kind === 'risks') {
      const riskRows = Array.isArray(reportData) ? reportData : [];
      const conTratamiento = riskRows.filter((item) => Boolean(item.diligencioTratamiento)).length;
      const sinTratamiento = Math.max(riskRows.length - conTratamiento, 0);
      return [
        { label: 'Proyectos con cierre', value: riskRows.length, tone: 'danger' },
        { label: 'Diligenciaron tratamiento', value: conTratamiento, tone: 'success' },
        { label: 'No diligenciaron', value: sinTratamiento, tone: 'warning' },
        { label: 'Filtro', value: 'Proyectos con cierre', tone: 'neutral' },
      ];
    }

    return [];
  }, [selectedBehavior, reportData, projects, selectedProject]);

  const handleProjectChange = (projectId) => {
    setSelectedProjectId(projectId);
  };

  const triggerBlobDownload = (blob, fileName) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const closePreviewModal = () => {
    setPreviewModal((current) => {
      if (current.url) {
        window.URL.revokeObjectURL(current.url);
      }
      return { open: false, title: '', url: '' };
    });
  };

  const openPdfPreview = async () => {
    if (!selectedBehaviorDownload) return;
    setReportActionBusy(true);
    setReportError('');
    try {
      const blob = await selectedBehaviorDownload();
      const previewUrl = window.URL.createObjectURL(blob);
      setPreviewModal((current) => {
        if (current.url) {
          window.URL.revokeObjectURL(current.url);
        }
        return {
          open: true,
          title: selectedReport?.nombre || selectedBehavior?.title || 'Reporte',
          url: previewUrl,
        };
      });
    } catch {
      setReportError('No fue posible generar la vista previa del PDF.');
    } finally {
      setReportActionBusy(false);
    }
  };

  const handleDownload = async () => {
    if (!selectedBehaviorDownload) return;
    setDownloadBusy(true);
    setReportError('');
    try {
      const filename = selectedBehavior?.filename(effectiveProjectId) || 'reporte.pdf';
      const blob = await selectedBehaviorDownload();
      triggerBlobDownload(blob, filename);
    } catch {
      setReportError('No fue posible descargar el reporte.');
    } finally {
      setDownloadBusy(false);
    }
  };

  const handleSecondaryDownload = async () => {
    if (selectedReportId !== 'TODOS_LOS_PROYECTOS' || !selectedBehavior?.downloadExcel) {
      return;
    }

    setDownloadBusy(true);
    try {
      const blob = await selectedBehavior.downloadExcel();
      triggerBlobDownload(blob, 'analitica-portafolio.xlsx');
    } catch (error) {
      setReportError(error?.response?.data?.detail || 'No fue posible descargar la exportación Excel.');
    } finally {
      setDownloadBusy(false);
    }
  };

  return (
    <div className="reports-page">
      <header className="reports-page__topbar">
        <div className="reports-page__brand">
          <p className="reports-page__eyebrow">Reportes Ejecutivos</p>
          <h1>Centro de Análisis y Consultas</h1>
        </div>

        <div className="reports-page__topbar-actions">
          <div className="reports-page__search-box">
            <Search size={18} />
            <input
              type="text"
              placeholder="Filtrar modelos de reporte..."
              value={reportQuery}
              onChange={(event) => setReportQuery(event.target.value)}
            />
          </div>

          <button
            type="button"
            className="reports-page__ghost-button"
            onClick={() => {
              setReportQuery('');
              setProjectQuery('');
              setSelectedProjectId('');
            }}
          >
            Limpiar
          </button>

        </div>
      </header>

      <section className="reports-page__model-section">
        <div className="reports-page__section-head">
          <div>
            <h2>Modelos de Reporte</h2>
            <p>Seleccione un formato para configurar su consulta corporativa.</p>
          </div>
        </div>

        <div className="reports-page__model-grid">
          {filteredReports.map((report) => {
            const behavior = REPORT_BEHAVIORS[report.id];
            const Icon = behavior?.previewIcon || FileText;
            const active = selectedReportId === report.id;

            return (
              <button
                key={report.id}
                type="button"
                className={`reports-page__model-card ${active ? 'is-active' : ''}`}
                onClick={() => setSelectedReportId(report.id)}
              >
                <div className="reports-page__model-card-icon">
                  <Icon size={18} />
                </div>
                <div className="reports-page__model-card-copy">
                  <div className="reports-page__model-card-head">
                    <strong>{report.nombre}</strong>
                    <span className={`reports-page__model-card-dot ${active ? 'is-active' : ''}`} />
                  </div>
                  <p>{report.descripcion}</p>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      <div className="reports-page__workspace">
        <section className="reports-page__config-card">
          <div className="reports-page__card-heading">
            <div>
              <p className="reports-page__panel-kicker">Configuración del Informe</p>
              <h2>{selectedReport?.nombre || 'Reporte seleccionado'}</h2>
            </div>
            <span className="reports-page__status-badge">
              {reportContext?.statusLabel || 'CONFIGURACIÓN VÁLIDA'}
            </span>
          </div>

          <p className="reports-page__hero-description">
            {selectedReport?.descripcion || 'Seleccione una plantilla para ver su configuración y salida.'}
          </p>

          <div className="reports-page__config-grid">
            <div className="reports-page__field">
              <label>Seleccionar Proyecto Principal</label>
              {selectedBehavior?.requiresProject ? (
                <>
                  <div className="reports-page__project-select">
                    <div className="reports-page__project-select-main">
                      <strong>{selectedProject?.nombre || 'Seleccione un proyecto'}</strong>
                      <span>{selectedProject ? `${selectedProject.id} · ${selectedProject.dependencia || 'Sin dependencia'}` : 'Requerido para este reporte'}</span>
                    </div>
                    <ChevronRight size={18} />
                  </div>
                  <div className="reports-page__project-search">
                    <Search size={16} />
                    <input
                      type="text"
                      placeholder="Buscar proyecto..."
                      value={projectQuery}
                      onChange={(event) => setProjectQuery(event.target.value)}
                    />
                  </div>
                  <div className="reports-page__project-results">
                    {loadingProjects ? (
                      <div className="reports-page__project-empty">Cargando proyectos...</div>
                    ) : filteredProjects.length > 0 ? (
                      filteredProjects.map((project) => {
                        const isActive = project.id === effectiveProjectId;
                        return (
                          <button
                            key={project.id}
                            type="button"
                            className={`reports-page__project-chip ${isActive ? 'is-active' : ''}`}
                            onClick={() => handleProjectChange(project.id)}
                          >
                            <span className="reports-page__project-chip-main">{project.id}</span>
                            <span className="reports-page__project-chip-sub">
                              {project.nombre} · {project.dependencia}
                            </span>
                          </button>
                        );
                      })
                    ) : (
                      <div className="reports-page__project-empty">No hay proyectos que coincidan con la búsqueda.</div>
                    )}
                  </div>
                </>
              ) : (
                <div className="reports-page__readonly-field">
                  <strong>Portafolio activo</strong>
                  <span>Este informe no requiere seleccionar un proyecto individual.</span>
                </div>
              )}
            </div>

            <div className="reports-page__field">
              <label>Nivel de Detalle Administrativo</label>
              <div className="reports-page__segmented-control">
                <button
                  type="button"
                  className={`reports-page__segmented-button ${detailMode === 'resumido' ? 'is-active' : ''}`}
                  onClick={() => setDetailMode('resumido')}
                >
                  Resumido
                </button>
                <button
                  type="button"
                  className={`reports-page__segmented-button ${detailMode === 'detallado' ? 'is-active' : ''}`}
                  onClick={() => setDetailMode('detallado')}
                >
                  Detallado
                </button>
              </div>

              <div className="reports-page__field-meta">
                <span>ID de proyecto:</span>
                <strong>{effectiveProjectId || 'No aplica'}</strong>
              </div>

              <div className={`reports-page__detail-note ${detailMode === 'detallado' ? 'is-detailed' : ''}`}>
                <strong>{reportContext?.detailModeLabel || 'Vista resumida'}</strong>
                <span>
                  {detailMode === 'detallado'
                    ? 'Se mostrarán más datos administrativos y un resumen de salida más completo.'
                    : 'Se mostrará solo la información esencial para consulta rápida.'}
                </span>
              </div>
            </div>
          </div>

          {reportError && (
            <section className="reports-page__alert">
              <ShieldAlert size={18} />
              <span>{reportError}</span>
            </section>
          )}

          {reportCards.length > 0 && (
            <section className="reports-page__metrics">
              {reportCards.map((card) => (
                card.topDeps ? (
                  <article key={card.label} className={`reports-page__metric-card tone-${card.tone} reports-page__metric-card--top3`}>
                    <span>{card.label}</span>
                    <div className="reports-page__top3-list">
                      {card.topDeps.length > 0 ? card.topDeps.map((dep, idx) => (
                        <div key={dep.dependencia} className="reports-page__top3-item">
                          <span className="reports-page__top3-rank">{idx + 1}</span>
                          <span className="reports-page__top3-dep">{dep.dependencia}</span>
                          <strong className="reports-page__top3-count">{dep.count}</strong>
                        </div>
                      )) : <span className="reports-page__top3-empty">Sin datos</span>}
                    </div>
                  </article>
                ) : (
                  <article key={card.label} className={`reports-page__metric-card tone-${card.tone}`}>
                    <span>{card.label}</span>
                    <strong>{card.value}</strong>
                  </article>
                )
              ))}
            </section>
          )}
        </section>

        <aside className="reports-page__summary-card">
          <span className="reports-page__summary-kicker">RESUMEN DE SALIDA</span>
          <h3>{selectedReport?.nombre || 'Reporte seleccionado'}</h3>
          <p>{selectedReport?.descripcion || 'Vista ejecutiva del reporte en curso.'}</p>

          <div className="reports-page__summary-callout">
            <strong>
              {selectedBehavior?.requiresProject ? 'El reporte se generará con un proyecto específico.' : 'El reporte se genera a nivel de portafolio.'}
            </strong>
            <span>Actualizado al {currentDateLabel}</span>
          </div>

          <dl className="reports-page__summary-meta">
            <div>
              <dt>FORMATO</dt>
              <dd>PDF Ejecutivo</dd>
            </div>
            <div>
              <dt>CONFIDENCIALIDAD</dt>
              <dd>Nivel 4 (Interno)</dd>
            </div>
          </dl>

          <div className="reports-page__summary-actions">
            <button
              type="button"
              className="reports-page__summary-button reports-page__summary-button--light"
              onClick={openPdfPreview}
              disabled={reportActionBusy || downloadBusy || !reportData || loadingReport}
            >
              <Eye size={16} />
              {reportActionBusy ? 'Previsualizando...' : 'Previsualizar Reporte'}
            </button>
            <button
              type="button"
              className="reports-page__summary-button"
              onClick={handleDownload}
              disabled={reportActionBusy || downloadBusy || !reportData || loadingReport}
            >
              <Download size={16} />
              {downloadBusy ? 'Generando...' : 'Generar y Descargar'}
            </button>
            {selectedReportId === 'TODOS_LOS_PROYECTOS' && (
              <button
                type="button"
                className="reports-page__summary-button reports-page__summary-button--ghost"
                onClick={handleSecondaryDownload}
                disabled={downloadBusy || loadingReport}
              >
                <FileSpreadsheet size={16} />
                Exportar Excel
              </button>
            )}
          </div>

          <div className="reports-page__summary-footnote">
            <span>{reportContext?.statusLabel || 'Sin incidencia'}</span>
            <span>{reportContext?.detailLabel || 'Salida lista para validación'}</span>
          </div>
        </aside>
      </div>

      {previewModal.open && (
        <div
          className="reports-page__modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closePreviewModal();
            }
          }}
        >
          <div className="reports-page__modal" onMouseDown={(event) => event.stopPropagation()}>
            <div className="reports-page__modal-header">
              <div>
                <span className="reports-page__modal-kicker">Vista previa PDF</span>
                <h3>{previewModal.title}</h3>
              </div>
              <button
                type="button"
                className="reports-page__icon-button"
                onClick={closePreviewModal}
              >
                <X size={18} />
              </button>
            </div>
            <iframe
              className="reports-page__pdf-frame"
              src={previewModal.url}
              title={previewModal.title}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default ReportsPage;


