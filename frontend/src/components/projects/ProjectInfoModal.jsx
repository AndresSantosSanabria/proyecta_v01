import { useRef, useState, useEffect, useCallback } from 'react';
import { X, ChevronDown, ChevronUp, FileText, Download, Upload, Users, Target, Layers, Shield, ClipboardList, LoaderCircle, Eye, History } from 'lucide-react';
import documentService from '../../services/documentService';
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
        {open ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
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
  if (blob.type === 'application/pdf') return true;
  try {
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

  return (
    <div className="pim-viewer-overlay" role="presentation" onClick={onClose}>
      <div className="pim-viewer-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
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
          {!isPdf && objectUrl ? (
            <div className="pim-viewer-fallback-block">
              <FileText size={48} strokeWidth={1} />
              <p className="pim-viewer-fallback-title">Este tipo de archivo no se puede previsualizar</p>
              <p className="pim-viewer-fallback-hint">{nombre}</p>
              <a href={objectUrl} download={nombre} className="pim-viewer-fallback-download">
                <Download size={14} /> Descargar archivo
              </a>
            </div>
          ) : objectUrl ? (
            <object data={objectUrl} type="application/pdf" className="pim-viewer-iframe">
              <p className="pim-viewer-fallback">No se pudo previsualizar. <a href={objectUrl} target="_blank" rel="noreferrer">Abrir en nueva pestana</a></p>
            </object>
          ) : (
            <div className="pim-viewer-loading">
              <LoaderCircle size={24} className="animate-spin" />
              <span>Cargando documento...</span>
            </div>
          )}
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
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `${tipoDocumento}_v${numeroVersion}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
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
        setViewerError(`La version ${numeroVersion} ya no esta disponible en el servidor.`);
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

  return (
    <div className="pim-viewer-overlay" role="presentation" onClick={onClose}>
      <div className="pim-viewer-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
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
          {loading ? (
            <p className="pim-empty"><LoaderCircle size={14} className="animate-spin" /> Cargando historial...</p>
          ) : error ? (
            <p className="pim-empty pim-version-error">{error}</p>
          ) : versiones.length === 0 ? (
            <p className="pim-empty">No hay versiones registradas.</p>
          ) : (
            versiones.map((v) => (
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
            ))
          )}
        </div>
      </div>
      {viewerError && !viewerBlob && (
        <div className="pim-viewer-overlay" role="presentation" onClick={handleCloseViewer}>
          <div className="pim-viewer-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
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

const DocumentLink = ({ proyectoId, tipoDocumento, nombre, onUploaded }) => {
  const inputRef = useRef(null);
  const [downloading, setDownloading] = useState(false);
  const [viewerBlob, setViewerBlob] = useState(null);
  const [loadingViewer, setLoadingViewer] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [versionCount, setVersionCount] = useState(0);
  const [showReplace, setShowReplace] = useState(false);
  const [observacion, setObservacion] = useState('');
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchCount = async () => {
      if (!proyectoId || !tipoDocumento) return;
      try {
        const response = await documentService.listarVersiones(proyectoId, tipoDocumento);
        const data = response?.data?.versiones || response?.versiones || [];
        setVersionCount(data.length);
      } catch {
        setVersionCount(0);
      }
    };
    fetchCount();
  }, [proyectoId, tipoDocumento, onUploaded]);

  const handleDownload = async () => {
    if (!proyectoId || !tipoDocumento) return;
    try {
      setDownloading(true);
      const blob = await documentService.descargarDocumento(proyectoId, tipoDocumento);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', nombre || `${tipoDocumento.toLowerCase()}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      window.URL.revokeObjectURL(url);
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

  const handleReplaceFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!observacion.trim()) {
      setError('La observacion es obligatoria al reemplazar un documento.');
      return;
    }
    try {
      setUploading(true);
      setError('');
      await documentService.cargarDocumento(proyectoId, tipoDocumento, file, observacion.trim());
      setObservacion('');
      setShowReplace(false);
      onUploaded?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.response?.data?.message || err?.message || 'Error al cargar el archivo.';
      setError(detail);
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const handleCancelReplace = () => {
    setShowReplace(false);
    setObservacion('');
    setError('');
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
      {showReplace && (
        <div className="pim-observacion-form">
          <label className="pim-observacion-label">
            Motivo del cambio (obligatorio):
            <textarea
              className="pim-observacion-input"
              value={observacion}
              onChange={(e) => setObservacion(e.target.value)}
              placeholder="Ingrese la razon por la cual se reemplaza el documento..."
              rows={2}
              maxLength={1000}
            />
          </label>
          <div className="pim-observacion-actions">
            <button type="button" className="pim-btn-cancel" onClick={handleCancelReplace} disabled={uploading}>
              Cancelar
            </button>
            <button
              type="button"
              className="pim-btn-upload"
              onClick={() => inputRef.current?.click()}
              disabled={uploading || !observacion.trim()}
            >
              {uploading ? <LoaderCircle size={14} className="animate-spin" /> : <Upload size={14} />}
              <span>{uploading ? 'Cargando...' : 'Seleccionar archivo'}</span>
            </button>
          </div>
          <input ref={inputRef} type="file" accept=".pdf" onChange={handleReplaceFile} hidden />
          {error && <span className="pim-doc-upload-error">{error}</span>}
        </div>
      )}
      {!showReplace && (
        <button type="button" className="pim-doc-replace-btn" onClick={() => setShowReplace(true)}>
          <Upload size={12} />
          <span>Reemplazar</span>
        </button>
      )}
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

const DocumentUpload = ({ proyectoId, tipoDocumento, nombre, onUploaded, exists }) => {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [observacion, setObservacion] = useState('');
  const [showObservacion, setShowObservacion] = useState(false);

  const handleFile = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (exists && !showObservacion) {
      setShowObservacion(true);
      return;
    }
    if (exists && (!observacion || !observacion.trim())) {
      setError('La observacion es obligatoria al reemplazar un documento.');
      return;
    }
    try {
      setUploading(true);
      setError('');
      await documentService.cargarDocumento(proyectoId, tipoDocumento, file, observacion || null);
      setObservacion('');
      setShowObservacion(false);
      onUploaded?.();
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.response?.data?.message || err?.message || 'Error al cargar el archivo.';
      setError(detail);
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = '';
    }
  };

  const handleCancelObservation = () => {
    setShowObservacion(false);
    setObservacion('');
    setError('');
  };

  return (
    <div className="pim-doc-upload-row">
      {showObservacion && exists && (
        <div className="pim-observacion-form">
          <label className="pim-observacion-label">
            Motivo del cambio (obligatorio):
            <textarea
              className="pim-observacion-input"
              value={observacion}
              onChange={(e) => setObservacion(e.target.value)}
              placeholder="Ingrese la razon por la cual se reemplaza el documento..."
              rows={2}
              maxLength={1000}
            />
          </label>
          <div className="pim-observacion-actions">
            <button type="button" className="pim-btn-cancel" onClick={handleCancelObservation} disabled={uploading}>
              Cancelar
            </button>
            <button
              type="button"
              className="pim-btn-upload"
              onClick={() => inputRef.current?.click()}
              disabled={uploading || !observacion.trim()}
            >
              {uploading ? <LoaderCircle size={14} className="animate-spin" /> : <Upload size={14} />}
              <span>{uploading ? 'Cargando...' : 'Confirmar reemplazo'}</span>
            </button>
          </div>
        </div>
      )}
      {!showObservacion && (
        <button
          type="button"
          className="pim-doc-upload"
          onClick={() => {
            if (exists) {
              setShowObservacion(true);
            } else {
              inputRef.current?.click();
            }
          }}
          disabled={uploading}
        >
          {uploading ? <LoaderCircle size={14} className="animate-spin" /> : <Upload size={14} />}
          <span>{nombre}</span>
          <span className="pim-doc-upload-hint">{exists ? 'Reemplazar archivo' : 'Cargar archivo'}</span>
        </button>
      )}
      <input ref={inputRef} type="file" accept=".pdf" onChange={handleFile} hidden />
      {error && <span className="pim-doc-upload-error">{error}</span>}
    </div>
  );
};

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

const ProjectInfoModal = ({ project, open, onClose, onDocumentUploaded }) => {
  const [existingDocs, setExistingDocs] = useState({});
  const [loadingDocs, setLoadingDocs] = useState(false);

  const proyectoId = project?.codigo || project?.id;

  const fetchDocuments = useCallback(async () => {
    if (!proyectoId) return;
    try {
      setLoadingDocs(true);
      const response = await documentService.listarDocumentos(proyectoId);
      const docs = response?.data?.documentos || response?.documentos || [];
      const map = {};
      docs.forEach((doc) => {
        const codigo = doc.tipoDocumentoCodigo || doc.tipoDocumento;
        if (codigo) map[codigo] = doc;
      });
      setExistingDocs(map);
    } catch {
      setExistingDocs({});
    } finally {
      setLoadingDocs(false);
    }
  }, [proyectoId]);

  useEffect(() => {
    if (open && proyectoId) {
      fetchDocuments();
    }
  }, [open, proyectoId, fetchDocuments]);

  if (!open || !project) return null;

  const patrocinador = project.patrocinador || {};
  const equipoTrabajo = Array.isArray(project.equipoTrabajo) ? project.equipoTrabajo : [];
  const fases = Array.isArray(project.fases) ? project.fases : [];
  const objetivos = Array.isArray(project.objetivosEspecificos) ? project.objetivosEspecificos : [];
  const furag = project.furag || {};

  const handleDocumentUploaded = () => {
    fetchDocuments();
    onDocumentUploaded?.();
  };

  return (
    <div className="pim-overlay" role="presentation" onClick={onClose}>
      <div className="pim-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <header className="pim-header">
          <div>
            <span className="pim-kicker">Informacion del proyecto</span>
            <h2>{project.nombre || project.nombreProyecto || 'Proyecto'}</h2>
            <p>{project.codigo || ''} · {project.dependencia || ''}</p>
          </div>
          <button type="button" className="pim-close" onClick={onClose} aria-label="Cerrar">
            <X size={20} />
          </button>
        </header>

        <div className="pim-body">
          <Section title="Datos generales" icon={ClipboardList}>
            <div className="pim-grid">
              <Field label="Dependencia" value={project.dependencia} />
              <Field label="Fecha de inicio" value={project.fechaInicio} />
              <Field label="Presupuesto estimado" value={project.presupuestoEstimado != null ? `$${Number(project.presupuestoEstimado).toLocaleString('es-CO')}` : null} />
              <Field label="Estado" value={project.estado} />
            </div>
            <div className="pim-field pim-field-wide">
              <span className="pim-field-label">Alcance detallado</span>
              <p className="pim-field-text">{project.alcanceDetallado || '—'}</p>
            </div>
            {objetivos.length > 0 && (
              <div className="pim-field pim-field-wide">
                <span className="pim-field-label">Objetivos especificos</span>
                <ul className="pim-list">
                  {objetivos.map((obj, i) => <li key={i}>{obj}</li>)}
                </ul>
              </div>
            )}
          </Section>

          <Section title="Patrocinador y equipo" icon={Users}>
            <div className="pim-grid">
              <Field label="Nombre" value={patrocinador.nombre} />
              <Field label="Cargo" value={patrocinador.cargo} />
              <Field label="Entidad" value={patrocinador.entidad} />
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
          </Section>

          <Section title="Fases, hitos y entregables" icon={Layers} defaultOpen={false}>
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
              <Field label="Plan de comunicaciones" value={project.tienePlanComunicaciones ? 'Si' : 'No'} />
            </div>
          </Section>

          <Section title="FURAG" icon={Target} defaultOpen={false}>
            <div className="pim-grid">
              <Field label="Infraestructura de datos" value={mapFuragValue(furag.infraestructuraDatos)} />
              <Field label="Interoperabilidad" value={mapFuragValue(furag.interoperabilidad)} />
              <Field label="Digitalizacion/Automatizacion" value={mapFuragValue(furag.digitalizacionAutomatizacion)} />
              <Field label="Contratacion publica" value={mapFuragValue(furag.contratacionPublica)} />
              <Field label="Servicios en la nube" value={mapFuragValue(furag.serviciosNube)} />
              <Field label="Sandbox regulatorio" value={mapFuragValue(furag.sandbox)} />
              <Field label="Tecnologias emergentes" value={mapFuragValue(furag.tecnologiasEmergentes)} />
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
                      onUploaded={handleDocumentUploaded}
                    />
                  ) : (
                    <DocumentUpload
                      key={doc.key}
                      proyectoId={proyectoId}
                      tipoDocumento={doc.tipo}
                      nombre={doc.nombre}
                      onUploaded={handleDocumentUploaded}
                      exists={false}
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
