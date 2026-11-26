import { useRef, useState, useEffect, useCallback } from 'react';
import { X, ChevronDown, ChevronUp, FileText, Download, Users, Target, Layers, Shield, ClipboardList, LoaderCircle, Eye, History } from 'lucide-react';
import documentService from '../../services/documentService';
import projectService from '../../services/projectService';
import configCatalogService from '../../services/configCatalogService';
import './ProjectInfoModal.css';

const Section = ({ title, icon: Icon, children, defaultOpen = true }) => {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="pim-section">
      <button type="button" className="pim-section-header" onClick={() => setOpen(!open)}>
        <div className="pim-section-title">
          <Icon size={16} />
          <span>{title}</span>
        </div>
        <div className="pim-section-actions">
          {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
        </div>
      </button>
      {open && <div className="pim-section-body">{children}</div>}
    </div>
  );
};

const Field = ({ label, value }) => (
  <div className="pim-field">
    <span className="pim-field-label">{label}</span>
    <strong className="pim-field-value">{value || '—'}</strong>
  </div>
);

const detectPdfFromBlob = async (blob) => {
  if (!blob) return false;
  try {
    // CWE-79: no se confia en blob.type (lo declara el servidor/cliente);
    // se exige siempre la firma real %PDF-.
    const buf = await blob.slice(0, 5).arrayBuffer();
    const header = new Uint8Array(buf);
    return header[0] === 0x25 && header[1] === 0x50 && header[2] === 0x44 && header[3] === 0x46;
  } catch {
    return false;
  }
};

const DocumentViewer = ({ blob, nombre, onClose }) => {
  const [objectUrl, setObjectUrl] = useState(null);
  const [isPdf, setIsPdf] = useState(false);
  const blobUrlRef = useRef(null);

  useEffect(() => {
    let cancelled = false;
    if (blobUrlRef.current) {
      URL.revokeObjectURL(blobUrlRef.current);
      blobUrlRef.current = null;
    }
    setObjectUrl(null);
    if (blob) {
      detectPdfFromBlob(blob).then((detected) => {
        if (cancelled) return;
        setIsPdf(detected);
        const mimeType = detected ? 'application/pdf' : blob.type || 'application/octet-stream';
        blob.arrayBuffer().then((buf) => {
          if (cancelled) return;
          const typedBlob = new Blob([buf], { type: mimeType });
          const url = URL.createObjectURL(typedBlob);
          blobUrlRef.current = url;
          setObjectUrl(url);
        });
      });
    }
    return () => {
      cancelled = true;
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
    };
  }, [blob]);

  useEffect(() => {
    const handleEsc = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  if (!blob) return null;

  let viewerContent;
  if (!isPdf && objectUrl) {
    viewerContent = (
      <div className="pim-viewer-fallback-block">
        <FileText size={48} strokeWidth={1} />
        <p className="pim-viewer-fallback-title">Este tipo de archivo no se puede previsualizar</p>
        <p className="pim-viewer-fallback-hint">{nombre}</p>
        <a href={objectUrl} download={nombre} className="pim-viewer-fallback-download">
          <Download size={14} /> Descargar archivo
        </a>
      </div>
    );
  } else if (objectUrl) {
    viewerContent = (
      <object data={objectUrl} type="application/pdf" className="pim-viewer-iframe">
        <p className="pim-viewer-fallback">No se pudo previsualizar. <a href={objectUrl} target="_blank" rel="noopener noreferrer">Abrir en nueva pestana</a></p>
      </object>
    );
  } else {
    viewerContent = (
      <div className="pim-viewer-loading">
        <LoaderCircle size={24} className="animate-spin" />
        <span>Cargando documento...</span>
      </div>
    );
  }

  return (
    <div className="pim-viewer-overlay" role="presentation" onClick={onClose}>
      <div className="pim-viewer-modal" role="dialog" aria-modal="true" tabIndex={0} onClick={(e) => e.stopPropagation()} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') e.stopPropagation(); }}>
        <header className="pim-viewer-header">
          <div className="pim-viewer-title">
            <FileText size={16} />
            <span>{nombre || 'Documento'}</span>
          </div>
          <button type="button" className="pim-viewer-close" onClick={onClose} aria-label="Cerrar visor">
            <X size={18} />
          </button>
        </header>
        <div className="pim-viewer-body">
          {viewerContent}
        </div>
      </div>
    </div>
  );
};

const VersionHistoryPanel = ({ proyectoId, tipoDocumento, nombreDocumento, onClose }) => {
  const [versiones, setVersiones] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [viewerBlob, setViewerBlob] = useState(null);
  const [viewerNombre, setViewerNombre] = useState('');
  const [viewerError, setViewerError] = useState('');

  useEffect(() => {
    const fetchVersiones = async () => {
      if (!proyectoId || !tipoDocumento) return;
      try {
        setLoading(true);
        setError('');
        const response = await documentService.listarVersiones(proyectoId, tipoDocumento);
        const data = response?.data?.versiones || response?.versiones || [];
        setVersiones(data);
      } catch {
        setError('Error al cargar el historial de versiones.');
      } finally {
        setLoading(false);
      }
    };
    fetchVersiones();
  }, [proyectoId, tipoDocumento]);

  useEffect(() => {
    const handleEsc = (e) => { if (e.key === 'Escape') onClose(); };
    document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, [onClose]);

  const handleDownloadVersion = async (numeroVersion) => {
    try {
      const blob = await documentService.descargarVersion(proyectoId, tipoDocumento, numeroVersion);
      const url = globalThis.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${tipoDocumento}_v${numeroVersion}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      globalThis.URL.revokeObjectURL(url);
    } catch {
      console.error('Error descargando version');
    }
  };

  const handleViewVersion = async (numeroVersion, nombre) => {
    try {
      setViewerBlob(null);
      setViewerError('');
      const blob = await documentService.descargarVersion(proyectoId, tipoDocumento, numeroVersion);
      setViewerBlob(blob);
      setViewerNombre(`${nombre || tipoDocumento} - Version ${numeroVersion}`);
    } catch (err) {
      console.error('Error cargando version para visor', err);
      const status = err?.response?.status;
      if (status === 404) {
        setViewerError(`La versión ${numeroVersion} ya no está disponible en el servidor.`);
      } else {
        setViewerError(`No se pudo cargar la version ${numeroVersion}.`);
      }
    }
  };

  const handleCloseViewer = () => {
    setViewerBlob(null);
    setViewerNombre('');
    setViewerError('');
  };

  let historyContent;
  if (loading) {
    historyContent = <p className="pim-empty"><LoaderCircle size={14} className="animate-spin" /> Cargando historial...</p>;
  } else if (error) {
    historyContent = <p className="pim-empty pim-version-error">{error}</p>;
  } else if (versiones.length === 0) {
    historyContent = <p className="pim-empty">No hay versiones registradas.</p>;
  } else {
    historyContent = versiones.map((v) => (
      <div key={v.id} className={`pim-version-item ${v.actual ? 'pim-version-item--actual' : ''}`}>
        <div className="pim-version-info">
          <div className="pim-version-header-row">
            <span className="pim-version-number">v{v.numeroVersion}</span>
            {v.actual && <span className="pim-version-badge">Actual</span>}
            <span className="pim-version-date">{v.subidoEn}</span>
          </div>
          <span className="pim-version-filename">{v.nombreArchivo}</span>
          {v.observacion && (
            <span className="pim-version-observation">"{v.observacion}"</span>
          )}
          <span className="pim-version-meta">
            {v.subidoPor}{v.subidoRol ? ` · ${v.subidoRol}` : ''} · {v.tamanoFormateado}
          </span>
        </div>
        <div className="pim-version-actions">
          <button
            type="button"
            className="pim-version-btn"
            onClick={() => handleViewVersion(v.numeroVersion, v.nombreArchivo)}
            title="Ver documento"
          >
            <Eye size={14} />
          </button>
          <button
            type="button"
            className="pim-version-btn"
            onClick={() => handleDownloadVersion(v.numeroVersion)}
            title="Descargar version"
          >
            <Download size={14} />
          </button>
        </div>
      </div>
    ));
  }

  return (
    <div className="pim-viewer-overlay" role="presentation" onClick={onClose}>
      <div className="pim-viewer-modal" role="dialog" aria-modal="true" tabIndex={0} onClick={(e) => e.stopPropagation()} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') e.stopPropagation(); }}>
        <header className="pim-viewer-header">
          <div className="pim-viewer-title">
            <History size={16} />
            <span>Historial - {nombreDocumento}</span>
          </div>
          <button type="button" className="pim-viewer-close" onClick={onClose} aria-label="Cerrar historial">
            <X size={18} />
          </button>
        </header>
        <div className="pim-version-list">
          {historyContent}
        </div>
      </div>
      {viewerError && !viewerBlob && (
        <div className="pim-viewer-overlay" role="presentation" onClick={handleCloseViewer}>
          <div className="pim-viewer-modal" role="dialog" aria-modal="true" tabIndex={0} onClick={(e) => e.stopPropagation()} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') e.stopPropagation(); }}>
            <header className="pim-viewer-header">
              <div className="pim-viewer-title">
                <FileText size={16} />
                <span>{viewerError}</span>
              </div>
              <button type="button" className="pim-viewer-close" onClick={handleCloseViewer} aria-label="Cerrar">
                <X size={18} />
              </button>
            </header>
            <div className="pim-viewer-body">
              <div className="pim-viewer-fallback-block">
                <FileText size={48} strokeWidth={1} />
                <p className="pim-viewer-fallback-title">{viewerError}</p>
                <p className="pim-viewer-fallback-hint">Intenta descargar el archivo para consultarlo.</p>
              </div>
            </div>
          </div>
        </div>
      )}
      {viewerBlob && (
        <DocumentViewer blob={viewerBlob} nombre={viewerNombre} onClose={handleCloseViewer} />
      )}
    </div>
  );
};

const DocumentLink = ({ proyectoId, tipoDocumento, nombre }) => {
  const [downloading, setDownloading] = useState(false);
  const [viewerBlob, setViewerBlob] = useState(null);
  const [loadingViewer, setLoadingViewer] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [versionCount, setVersionCount] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const fetchCount = async () => {
      if (!proyectoId || !tipoDocumento) return;
      try {
        const response = await documentService.listarVersiones(proyectoId, tipoDocumento);
        if (controller.signal.aborted) return;
        const data = response?.data?.versiones || response?.versiones || [];
        setVersionCount(data.length);
      } catch {
        if (!controller.signal.aborted) setVersionCount(0);
      }
    };
    fetchCount();
    return () => controller.abort();
  }, [proyectoId, tipoDocumento]);

  const handleDownload = async () => {
    if (!proyectoId || !tipoDocumento) return;
    try {
      setDownloading(true);
      const blob = await documentService.descargarDocumento(proyectoId, tipoDocumento);
      const url = globalThis.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', nombre || `${tipoDocumento.toLowerCase()}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      globalThis.URL.revokeObjectURL(url);
    } catch {
      console.error('Error descargando documento');
    } finally {
      setDownloading(false);
    }
  };

  const handleView = async () => {
    if (!proyectoId || !tipoDocumento) return;
    try {
      setLoadingViewer(true);
      const blob = await documentService.descargarDocumento(proyectoId, tipoDocumento);
      setViewerBlob(blob);
    } catch {
      console.error('Error cargando documento para visor');
    } finally {
      setLoadingViewer(false);
    }
  };

  const handleCloseViewer = () => {
    setViewerBlob(null);
  };

  return (
    <>
      <div className="pim-doc-link-row">
        <button type="button" className="pim-doc-link" onClick={handleView} disabled={loadingViewer}>
          <FileText size={14} />
          <span>{nombre || tipoDocumento}</span>
          {versionCount > 0 && (
            <span className="pim-version-count" title={`${versionCount} version(es)`}>{versionCount}</span>
          )}
          {loadingViewer ? <LoaderCircle size={14} className="animate-spin" /> : <Eye size={14} />}
        </button>
        <button type="button" className="pim-doc-download-btn" onClick={handleDownload} disabled={downloading} title="Descargar">
          {downloading ? <LoaderCircle size={14} className="animate-spin" /> : <Download size={14} />}
        </button>
        <button type="button" className="pim-doc-history-btn" onClick={() => setShowHistory(true)} title="Historial de versiones">
          <History size={14} />
        </button>
      </div>
      {viewerBlob && (
        <DocumentViewer blob={viewerBlob} nombre={nombre} onClose={handleCloseViewer} />
      )}
      {showHistory && (
        <VersionHistoryPanel
          proyectoId={proyectoId}
          tipoDocumento={tipoDocumento}
          nombreDocumento={nombre}
          onClose={() => setShowHistory(false)}
        />
      )}
    </>
  );
};

const DocumentUnavailable = ({ nombre }) => (
  <div className="pim-doc-link-row">
    <div className="pim-doc-link">
      <FileText size={14} />
      <span>{nombre}</span>
    </div>
    <span className="pim-empty">No disponible</span>
  </div>
);

const DOC_TYPES = [
  { key: 'viabilizacionPdf', tipo: 'VIABILIZACION', nombre: 'Documento de viabilidad' },
  { key: 'actaConstitucionPdf', tipo: 'ACTA_CONSTITUCION', nombre: 'Acta de constitucion' },
  { key: 'cronogramaPdf', tipo: 'CRONOGRAMA', nombre: 'Cronograma del proyecto' },
  { key: 'planComunicacionesPdf', tipo: 'PLAN_COMUNICACIONES', nombre: 'Plan de comunicaciones' },
];

const mapFuragValue = (val) => {
  if (val === 'SI') return 'Sí';
  if (val === 'NO') return 'No';
  if (val === 'NA' || val === 'NO_APLICA') return 'No aplica';
  return val;
};

const DEFAULT_FURAG_LABELS = {
  infraestructuraDatos: '¿El proyecto incluye uso de infraestructura de datos (datos abiertos, big data, analytics)?',
  interoperabilidad: '¿El proyecto requiere interoperabilidad con otros sistemas de la entidad o del Estado?',
  digitalizacionAutomatizacion: '¿El proyecto contempla digitalización o automatización de procesos?',
  contratacionPublica: '¿El proyecto está relacionado con contratación pública electrónica?',
  serviciosNube: '¿El proyecto utilizará servicios en la nube (IaaS, PaaS, SaaS)?',
  sandbox: '¿El proyecto requiere un entorno Sandbox regulatorio para pruebas?',
  tecnologiasEmergentes: '¿El proyecto hace uso de tecnologías emergentes (IA, Blockchain, IoT)?',
};

const DEFAULT_FURAG_ORDER = [
  'infraestructuraDatos',
  'interoperabilidad',
  'digitalizacionAutomatizacion',
  'contratacionPublica',
  'serviciosNube',
  'sandbox',
  'tecnologiasEmergentes',
];

const normalizeFurag = (payload) => {
  const data = payload?.data?.data ?? payload?.data ?? payload ?? {};
  const source = data.respuestas || data;
  let detalle = [];
  if (Array.isArray(data.detalle)) {
    detalle = data.detalle;
  } else if (Array.isArray(source.detalle)) {
    detalle = source.detalle;
  }
  return {
    detalle,
    infraestructuraDatos: source.infraestructuraDatos ?? null,
    interoperabilidad: source.interoperabilidad ?? null,
    digitalizacionAutomatizacion: source.digitalizacionAutomatizacion ?? null,
    contratacionPublica: source.contratacionPublica ?? null,
    serviciosNube: source.serviciosNube ?? null,
    sandbox: source.sandbox ?? null,
    tecnologiasEmergentes: source.tecnologiasEmergentes ?? null,
  };
};

const normalizeFuragQuestions = (preguntas) => {
  if (!Array.isArray(preguntas)) return [];
  return preguntas
    .map((pregunta) => ({
      key: pregunta?.key || pregunta?.codigo || pregunta?.id || '',
      label: pregunta?.label || pregunta?.pregunta || pregunta?.nombre || '',
    }))
    .filter((pregunta) => pregunta.key && pregunta.label);
};

const normalizeFuragKey = (value) =>
  String(value || '')
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replaceAll(/[\u0300-\u036f]/g, '')
    .replaceAll(/[^a-z0-9]/g, '');

const resolveFuragAnswer = (furagMap, pregunta) => {
  if (!furagMap || !pregunta) return null;

  const candidates = [
    pregunta.key,
    pregunta.label,
    pregunta.codigo,
    pregunta.nombre,
    pregunta.pregunta,
    pregunta.key?.replace(/^furag[_-]?/i, ''),
  ].filter(Boolean);

  const normalizedCandidates = new Set(candidates.map(normalizeFuragKey));
  const entries = Object.entries(furagMap);

  for (const [entryKey, entryValue] of entries) {
    const normalizedEntryKey = normalizeFuragKey(entryKey);
    if (normalizedCandidates.has(normalizedEntryKey)) {
      return entryValue;
    }
  }

  if (pregunta.label) {
    const normalizedLabel = normalizeFuragKey(pregunta.label);
    for (const [entryKey, entryValue] of entries) {
      if (normalizeFuragKey(entryKey).includes(normalizedLabel) || normalizedLabel.includes(normalizeFuragKey(entryKey))) {
        return entryValue;
      }
    }
  }

  return null;
};

const ProjectInfoModal = ({ project, open, onClose }) => {
  const [existingDocs, setExistingDocs] = useState({});
  const [loadingDocs, setLoadingDocs] = useState(false);
  const [furag, setFurag] = useState(null);
  const [loadingFurag, setLoadingFurag] = useState(false);
  const [furagQuestions, setFuragQuestions] = useState(
    DEFAULT_FURAG_ORDER.map((key) => ({ key, label: DEFAULT_FURAG_LABELS[key] }))
  );
  const proyectoId = project?.codigo || project?.id;

  const fetchDocuments = useCallback(async (signal) => {
    if (!proyectoId) return;
    try {
      setLoadingDocs(true);
      const response = await documentService.listarDocumentos(proyectoId);
      if (signal?.aborted) return;
      const docs = response?.data?.documentos || response?.documentos || [];
      const map = {};
      docs.forEach((doc) => {
        const codigo = doc.tipoDocumentoCodigo || doc.tipoDocumento;
        if (codigo) map[codigo] = doc;
      });
      setExistingDocs(map);
    } catch {
      if (!signal?.aborted) setExistingDocs({});
    } finally {
      if (!signal?.aborted) setLoadingDocs(false);
    }
  }, [proyectoId]);

  const fetchFurag = useCallback(async () => {
    if (!proyectoId) return;
    try {
      setLoadingFurag(true);
      const response = await projectService.getFurag(proyectoId);
      setFurag(normalizeFurag(response));
    } catch {
      setFurag(normalizeFurag(project?.furag));
    } finally {
      setLoadingFurag(false);
    }
  }, [proyectoId, project?.furag]);

  const fetchFuragLabels = useCallback(async () => {
    try {
      const catalog = await configCatalogService.getPetiCatalog();
      const preguntas = normalizeFuragQuestions(catalog?.furagPreguntas);
      if (preguntas.length === 0) {
        setFuragQuestions(DEFAULT_FURAG_ORDER.map((key) => ({ key, label: DEFAULT_FURAG_LABELS[key] })));
        return;
      }
      setFuragQuestions(preguntas);
    } catch {
      setFuragQuestions(DEFAULT_FURAG_ORDER.map((key) => ({ key, label: DEFAULT_FURAG_LABELS[key] })));
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    if (open && proyectoId) {
      fetchDocuments(controller.signal);
      fetchFurag();
      fetchFuragLabels();
    }
    return () => controller.abort();
  }, [open, proyectoId, fetchDocuments, fetchFurag, fetchFuragLabels]);

  if (!open || !project) return null;

  const patrocinador = project.patrocinador || {};
  const equipoTrabajo = Array.isArray(project.equipoTrabajo) ? project.equipoTrabajo : [];
  const stakeholders = Array.isArray(project.stakeholders) ? project.stakeholders : [];
  const fases = Array.isArray(project.fases) ? project.fases : [];
  const objetivos = Array.isArray(project.objetivosEspecificos) ? project.objetivosEspecificos : [];
  const furagData = furag || normalizeFurag(project?.furag);
  const furagAnswers = furagData?.respuestas || furagData || {};
  const furagDetail = Array.isArray(furagData?.detalle) ? furagData.detalle : [];

  return (
    <div className="pim-overlay" role="presentation" onClick={onClose}>
      <div className="pim-modal" role="dialog" aria-modal="true" tabIndex={0} onClick={(e) => e.stopPropagation()} onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') e.stopPropagation(); }}>
        <header className="pim-header">
          <div>
            <span className="pim-kicker">Información del proyecto</span>
            <h2>{project.nombre || project.nombreProyecto || 'Proyecto'}</h2>
            <p>{project.codigo || ''} · {project.dependencia || ''}</p>
          </div>
          <button type="button" className="pim-close" onClick={onClose} aria-label="Cerrar">
            <X size={20} />
          </button>
        </header>

        <div className="pim-body">
          <Section title="Datos generales" icon={ClipboardList}>
            <>
                <div className="pim-grid">
                  <Field label="Dependencia" value={project.dependencia} />
                  <Field label="Fecha de inicio" value={project.fechaInicio} />
                  <Field label="Presupuesto estimado" value={project.presupuestoEstimado == null ? null : `$${Number(project.presupuestoEstimado).toLocaleString('es-CO')}`} />
                  <Field label="Estado" value={project.estado} />
                </div>
                <div className="pim-field pim-field-wide">
                  <span className="pim-field-label">Alcance</span>
                  <p className="pim-field-text">{project.alcanceDetallado || '\u2014'}</p>
                </div>
                {objetivos.length > 0 && (
                  <div className="pim-field pim-field-wide">
                    <span className="pim-field-label">Objetivos especificos</span>
                    <ul className="pim-list">
                      {objetivos.map((obj, i) => <li key={i}>{obj}</li>)}
                    </ul>
                  </div>
                )}
            </>
          </Section>

          <Section title="Patrocinador y equipo" icon={Users}>
            <>
                <div className="pim-grid">
                  <Field label="Nombre" value={patrocinador.nombre} />
                  <Field label="Cargo" value={patrocinador.cargo} />
                  <Field label="Proceso SIGC" value={patrocinador.procesoSigc} />
                  <Field label="Procedimiento SIGC" value={patrocinador.procedimientoSigc} />
                </div>
                {equipoTrabajo.length > 0 && (
                  <div className="pim-field pim-field-wide">
                    <span className="pim-field-label">Equipo de trabajo</span>
                    <div className="pim-team-list">
                      {equipoTrabajo.map((m, i) => (
                        <div key={i} className="pim-team-member">
                          <strong>{m.nombre}</strong>
                          <span>{m.cargo}{m.rol ? ` · ${m.rol}` : ''}</span>
                          {m.dependencia && <span>{m.dependencia}</span>}
                          {m.correo && <span>{m.correo}</span>}
                          {m.telefono && <span>{m.telefono}</span>}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {stakeholders.length > 0 && (
                  <div className="pim-field pim-field-wide">
                    <span className="pim-field-label">Grupo de Interes (Stakeholders)</span>
                    <div className="pim-team-list">
                      {stakeholders.map((s, i) => (
                        <div key={i} className="pim-team-member">
                          <strong>{s.rol}</strong>
                          {s.descripcion && <span>{s.descripcion}</span>}
                          {s.interes && <span><em>Interes:</em> {s.interes}</span>}
                          {s.impacto && <span><em>Impacto:</em> {s.impacto}</span>}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
            </>
          </Section>

          <Section title="Fases, hitos y entregables" icon={Layers} defaultOpen={false}>
            <>
                {fases.length > 0 ? fases.map((fase, fi) => (
                  <div key={fi} className="pim-phase">
                    <div className="pim-phase-header">
                      <strong>Fase {fi + 1}: {fase.nombre}</strong>
                      <span>{fase.ponderacion}%</span>
                    </div>
                    {(fase.hitos || []).map((hito, hi) => (
                      <div key={hi} className="pim-hito">
                        <div className="pim-hito-header">
                          <span>Hito {fi + 1}.{hi + 1}: {hito.nombre}</span>
                          <span>{hito.ponderacion}%</span>
                        </div>
                        {(hito.entregables || []).map((ent, ei) => (
                          <div key={ei} className="pim-entregable">
                            <span>{ent.nombre}</span>
                            <span>{ent.ponderacion}% · {ent.fechaLimite || 'Sin fecha'}</span>
                          </div>
                        ))}
                      </div>
                    ))}
                  </div>
                )) : <p className="pim-empty">No hay fases configuradas.</p>}
            </>
          </Section>

          <Section title="PETI" icon={Shield} defaultOpen={false}>
            <div className="pim-grid">
                <Field label="Proyecto en PETI" value={project.esPeti || project.peti ? 'Si' : 'No'} />
                {(project.esPeti || project.peti) && (
                  <>
                    <Field label="Vigencia" value={project.vigenciaPeti} />
                    <Field label="Estrategia" value={project.estrategiaPeti} />
                  </>
                )}
            </div>
          </Section>

          <Section title="FURAG" icon={Target} defaultOpen={false}>
            <div className="pim-furag-list">
              {loadingFurag ? (
                <p className="pim-empty"><LoaderCircle size={14} className="animate-spin" /> Cargando FURAG...</p>
              ) : (
                <>
                  {(furagDetail.length > 0 ? furagDetail : furagQuestions).map((pregunta) => {
                    const response = pregunta?.response ?? resolveFuragAnswer(furagAnswers, pregunta);
                    return (
                      <div key={pregunta.key || pregunta.label} className="pim-furag-item">
                        <span className="pim-field-label pim-furag-question">{pregunta.label}</span>
                        <strong className="pim-field-value pim-furag-answer">
                          {mapFuragValue(response) || '—'}
                        </strong>
                      </div>
                    );
                  })}
                </>
              )}
            </div>
          </Section>

          <Section title="Documentos" icon={FileText} defaultOpen={false}>
            <div className="pim-docs">
              {loadingDocs ? (
                <p className="pim-empty"><LoaderCircle size={14} className="animate-spin" /> Cargando documentos...</p>
              ) : (
                DOC_TYPES.map((doc) =>
                  existingDocs[doc.tipo] ? (
                    <DocumentLink
                      key={doc.key}
                      proyectoId={proyectoId}
                      tipoDocumento={doc.tipo}
                      nombre={doc.nombre}
                    />
                  ) : (
                    <DocumentUnavailable
                      key={doc.key}
                      nombre={doc.nombre}
                    />
                  )
                )
              )}
            </div>
          </Section>
        </div>
      </div>
    </div>
  );
};

export default ProjectInfoModal;





