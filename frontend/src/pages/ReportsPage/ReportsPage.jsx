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
  FolderSearch,
  Radar,
  RefreshCw,
  Search,
  ShieldAlert,
  TimerReset,
  X,
} from 'lucide-react';
import ReportDocumentPreview from '../../components/ReportDocumentPreview/ReportDocumentPreview';
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
    requiresProject: true,
    downloadLabel: 'Descargar PDF',
    previewIcon: BookText,
    kind: 'plan',
    filename: (projectId) => `reporte-plan-comunicaciones-${projectId}.pdf`,
    download: (projectId) => reportService.downloadPlanComunicacionesPdf(projectId),
    fetchData: (projectId) => reportService.getPlanComunicaciones(projectId),
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
    requiresProject: true,
    downloadLabel: 'Descargar PDF',
    previewIcon: Radar,
    kind: 'risks',
    filename: (projectId) => `reporte-verificacion-tratamiento-a-riesgos-${projectId}.pdf`,
    download: (projectId) => reportService.downloadRiesgosPdf(projectId),
    fetchData: (projectId) => reportService.getRiesgos(projectId),
  },
};

const normalizeRiskLevel = (value) => {
  const normalized = String(value || '').trim().toUpperCase();
  if (normalized === 'CRITICO') return 'EXTREMO';
  return normalized;
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
    descripcion: 'Consolidado institucional de respuestas FURAG por proyecto',
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

const countBy = (items, selector) =>
  items.reduce((acc, item) => {
    const key = selector(item) || 'SIN_DATO';
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});

const blankMetricCards = [
  { label: 'Proyecto', value: '—', tone: 'primary' },
  { label: 'Avance', value: '—', tone: 'neutral' },
  { label: 'Estado', value: '—', tone: 'neutral' },
  { label: 'Detalle', value: '—', tone: 'neutral' },
];

const ReportsPage = () => {
  const [reportConfigs, setReportConfigs] = useState([]);
  const [projects, setProjects] = useState([]);
  const [loadingProjects, setLoadingProjects] = useState(true);
  const [selectedReportId, setSelectedReportId] = useState('ESTADO_PROYECTO');
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [projectQuery, setProjectQuery] = useState('');
  const [reportData, setReportData] = useState(null);
  const [loadingReport, setLoadingReport] = useState(false);
  const [reportError, setReportError] = useState('');
  const [previewModal, setPreviewModal] = useState({ open: false, title: '', url: '' });
  const [downloadBusy, setDownloadBusy] = useState(false);
  const [reportActionBusy, setReportActionBusy] = useState(false);

  const availableReports = reportConfigs.length > 0 ? reportConfigs : FALLBACK_REPORTS;

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
    const source = query
      ? projects.filter((project) =>
          [project.id, project.nombre, project.dependencia, project.estado]
            .filter(Boolean)
            .some((value) => String(value).toLowerCase().includes(query))
        )
      : projects;
    return source.slice(0, 8);
  }, [projects, projectQuery]);

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
    return selectedBehavior.download;
  }, [selectedBehavior, effectiveProjectId]);

  const reportCards = useMemo(() => {
    if (!selectedBehavior || !reportData) {
      return [];
    }

    if (selectedBehavior.kind === 'project') {
      return [
        { label: 'Proyecto', value: reportData.nombre, tone: 'primary' },
        { label: 'Avance total', value: toPercent(reportData.avanceTotal), tone: 'success' },
        { label: 'Director', value: reportData.directorNombre, tone: 'neutral' },
        { label: 'Entregables vencidos', value: reportData.entregablesVencidos?.length ?? 0, tone: 'warning' },
      ];
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
      return [
        { label: 'Proyectos con retrasos', value: total, tone: 'danger' },
        { label: 'Atrasos acumulados', value: totalAtrasos, tone: 'warning' },
        { label: 'Dependencia afectada', value: total > 0 ? delayRows[0].dependencia : 'No aplica', tone: 'neutral' },
        { label: 'Proyectos revisados', value: projects.length, tone: 'primary' },
      ];
    }

    if (selectedBehavior.kind === 'plan') {
      return [
        { label: 'Proyecto', value: reportData.nombre, tone: 'primary' },
        { label: 'Dependencia', value: reportData.dependencia, tone: 'neutral' },
        { label: 'Plan PDF', value: reportData.planPdfUrl ? 'Disponible' : 'No cargado', tone: reportData.planPdfUrl ? 'success' : 'warning' },
        { label: 'Estado', value: reportData.planPdfUrl ? 'Listo para descargar' : 'Pendiente', tone: 'primary' },
      ];
    }

    if (selectedBehavior.kind === 'furag') {
      return [
        { label: 'Es PETI', value: reportData.esPeti ? 'SI' : 'NO', tone: reportData.esPeti ? 'success' : 'warning' },
        { label: 'Estrategia PETI', value: reportData.estrategiaPeti || 'No disponible', tone: 'neutral' },
        { label: 'Vigencia', value: reportData.vigenciaPeti || 'No disponible', tone: 'primary' },
        { label: 'Objetivo general', value: reportData.objetivoGeneral ? 'Definido' : 'No disponible', tone: 'primary' },
      ];
    }

    if (selectedBehavior.kind === 'risks') {
      const riskRows = Array.isArray(reportData) ? reportData : [];
      const porNivel = countBy(riskRows, (item) => normalizeRiskLevel(item.nivel));
      return [
        { label: 'Riesgos totales', value: riskRows.length, tone: 'danger' },
        { label: 'Bajo', value: porNivel.BAJO || 0, tone: 'success' },
        { label: 'Moderado', value: porNivel.MODERADO || 0, tone: 'warning' },
        { label: 'Alto / Extremo', value: `${porNivel.ALTO || 0} / ${porNivel.EXTREMO || 0}`, tone: 'danger' },
      ];
    }

    return [];
  }, [selectedBehavior, reportData, projects.length]);

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
    if (!selectedBehaviorDownload) {
      return;
    }

    setReportActionBusy(true);
    try {
      const blob = await selectedBehavior.download(effectiveProjectId);
      const previewUrl = window.URL.createObjectURL(blob);
      setPreviewModal({
        open: true,
        title: selectedReport?.nombre || selectedBehavior.title,
        url: previewUrl,
      });
    } catch (error) {
      setReportError(error?.response?.data?.detail || 'No fue posible abrir la vista previa del PDF.');
    } finally {
      setReportActionBusy(false);
    }
  };

  const handleDownload = async () => {
    if (!selectedBehaviorDownload) {
      return;
    }

    setDownloadBusy(true);
    try {
      const blob = await selectedBehavior.download(effectiveProjectId);
      triggerBlobDownload(blob, selectedBehavior.filename(effectiveProjectId));
    } catch (error) {
      setReportError(error?.response?.data?.detail || 'No fue posible descargar el reporte.');
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
      <div className="reports-page__header">
        <div>
          <p className="reports-page__eyebrow">Consultas ejecutivas</p>
          <h1>Reportes</h1>
          <p>Genere, previsualice y descargue los reportes institucionales según la plantilla seleccionada.</p>
        </div>
        <div className="reports-page__header-actions">
          <div className="reports-page__search-box">
            <Search size={18} />
            <input
              type="text"
              placeholder="Buscar proyecto..."
              value={projectQuery}
              onChange={(event) => setProjectQuery(event.target.value)}
            />
          </div>
          <button
            type="button"
            className="reports-page__ghost-button"
            onClick={() => {
              setProjectQuery('');
              setSelectedProjectId('');
            }}
          >
            <RefreshCw size={16} />
            Limpiar
          </button>
        </div>
      </div>

      <div className="reports-page__shell">
        <aside className="reports-page__sidebar">
          <div className="reports-page__sidebar-title">Tipo de Reporte</div>
          <div className="reports-page__radio-group">
            {availableReports.map((report) => {
              const behavior = REPORT_BEHAVIORS[report.id];
              const Icon = behavior?.previewIcon || FileText;
              const active = selectedReportId === report.id;

              return (
                <button
                  key={report.id}
                  type="button"
                  className={`reports-page__radio-card ${active ? 'is-active' : ''}`}
                  onClick={() => setSelectedReportId(report.id)}
                >
                  <span className={`reports-page__radio-dot ${active ? 'is-active' : ''}`} />
                  <span className="reports-page__radio-body">
                    <span className="reports-page__radio-top">
                      <Icon size={16} />
                      <strong>{report.nombre}</strong>
                    </span>
                    <span className="reports-page__radio-description">{report.descripcion}</span>
                  </span>
                  <ChevronRight size={16} className="reports-page__radio-chevron" />
                </button>
              );
            })}
          </div>
        </aside>

        <main className="reports-page__content">
          <section className="reports-page__panel">
            <div className="reports-page__panel-header">
              <div>
                <p className="reports-page__panel-kicker">Plantilla seleccionada</p>
                <h2>{selectedReport ? selectedReport.nombre : 'Reporte'}</h2>
                <p>{selectedReport?.descripcion || 'Seleccione una plantilla para ver su configuración y salida.'}</p>
              </div>
              <div className="reports-page__panel-pills">
                <span className="reports-page__pill">
                  {selectedBehavior?.requiresProject ? 'Requiere proyecto' : 'Portafolio'}
                </span>
                <span className="reports-page__pill reports-page__pill--outline">
                  {selectedReportId}
                </span>
              </div>
            </div>

            {selectedBehavior?.requiresProject && (
              <div className="reports-page__project-picker">
                <div className="reports-page__project-picker-header">
                  <div>
                    <span className="reports-page__project-picker-label">Proyecto</span>
                    <strong>{selectedProject?.nombre || 'Seleccione un proyecto'}</strong>
                  </div>
                  <button
                    type="button"
                    className="reports-page__secondary-button"
                    onClick={() => setSelectedProjectId(selectedProject?.id || '')}
                    disabled={!selectedProject}
                  >
                    <FolderSearch size={16} />
                    Usar seleccionado
                  </button>
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
              </div>
            )}

            <div className="reports-page__actions-row">
              <button
                type="button"
                className="reports-page__primary-button"
                onClick={openPdfPreview}
                disabled={reportActionBusy || downloadBusy || !selectedBehaviorDownload || loadingReport}
              >
                <Eye size={16} />
                Vista previa PDF
              </button>
              <button
                type="button"
                className="reports-page__download-button"
                onClick={handleDownload}
                disabled={reportActionBusy || downloadBusy || !selectedBehaviorDownload || loadingReport}
              >
                <Download size={16} />
                {downloadBusy ? 'Descargando...' : selectedBehavior?.downloadLabel || 'Descargar PDF'}
              </button>
              {selectedReportId === 'TODOS_LOS_PROYECTOS' && (
                <button
                  type="button"
                  className="reports-page__ghost-button reports-page__ghost-button--accent"
                  onClick={handleSecondaryDownload}
                  disabled={downloadBusy || loadingReport}
                >
                  <FileSpreadsheet size={16} />
                  Exportar Excel
                </button>
              )}
            </div>

            {reportError && (
              <div className="reports-page__alert">
                <ShieldAlert size={18} />
                <span>{reportError}</span>
              </div>
            )}
          </section>

          <section className="reports-page__metrics">
            {(reportCards.length > 0 ? reportCards : blankMetricCards).map((card) => (
              <article key={card.label} className={`reports-page__metric-card tone-${card.tone}`}>
                <span>{card.label}</span>
                <strong>{card.value}</strong>
              </article>
            ))}
          </section>

          <section className="reports-page__panel reports-page__panel--body">
            <div className="reports-page__panel-header reports-page__panel-header--compact">
              <div>
                <p className="reports-page__panel-kicker">Vista previa del documento</p>
                <h2>{selectedBehavior?.title || 'Reporte'}</h2>
              </div>
              <span className={`reports-page__pill ${loadingReport ? '' : 'reports-page__pill--outline'}`}>
                {loadingReport ? 'Actualizando...' : 'Listo'}
              </span>
            </div>

            {loadingReport && (
              <div className="reports-page__loading">Cargando información del reporte...</div>
            )}

            {!loadingReport && !reportData && (
              <div className="reports-page__empty">
                Seleccione una plantilla y, si aplica, un proyecto para ver la vista previa del documento.
              </div>
            )}

            {!loadingReport && reportData && (
              <ReportDocumentPreview
                reportId={selectedReportId}
                reportData={reportData}
                selectedProject={selectedProject}
              />
            )}
          </section>
        </main>
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
