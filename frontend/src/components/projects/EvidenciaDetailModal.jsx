import { useState, useEffect, useRef, useCallback } from 'react';
import {
  X, FileText, Eye, Calendar, User, Tag, Clock, AlertCircle, LoaderCircle,
  Info, Download, History, ChevronDown, ChevronUp,
} from 'lucide-react';
import apiClient from '../../api/axiosConfig';
import projectService from '../../services/projectService';
import documentService from '../../services/documentService';
import { formatDate, formatDateTime } from '../../utils/locale';
import './EvidenciaDetailModal.css';

const normalizeEvidencePath = (url) => {
  if (!url) return '';
  return String(url).trim()
    .replace(/^https?:\/\/[^/]+\/api\/v1/i, '')
    .replace(/^\/api\/v1/i, '')
    .replace(/^\//, '');
};

const getEvidenceBlob = async (url) => {
  const path = normalizeEvidencePath(url);
  if (!path) throw new Error('No se encontro la URL del archivo.');
  const response = await apiClient.get(path, { responseType: 'blob', headers: { Accept: 'application/pdf' } });
  return response.data;
};

const isPdfBlob = async (blob) => {
  if (!(blob instanceof Blob) || blob.size === 0) return false;
  try {
    const sig = new Uint8Array(await blob.slice(0, 5).arrayBuffer());
    return new TextDecoder('ascii').decode(sig).startsWith('%PDF-');
  } catch { return false; }
};

const estadoColors = {
  A_CONFORMIDAD: { bg: 'var(--success-soft)', color: 'var(--success)', label: 'A Conformidad' },
  APROBADO: { bg: 'var(--success-soft)', color: 'var(--success)', label: 'Aprobado' },
  PENDIENTE: { bg: 'var(--warning-soft)', color: 'var(--warning)', label: 'Pendiente' },
  EN_REVISION: { bg: 'var(--primary-soft)', color: 'var(--primary)', label: 'En Revision' },
  EN_PROCESO: { bg: 'var(--primary-soft)', color: 'var(--primary)', label: 'En Proceso' },
  RECHAZADO: { bg: 'var(--danger-soft)', color: 'var(--danger)', label: 'Rechazado' },
  COMPLETADO: { bg: 'var(--success-soft)', color: 'var(--success)', label: 'Completado' },
  CARGADO: { bg: 'var(--success-soft)', color: 'var(--success)', label: 'Cargado' },
};

const normalizeEstado = (estado) => {
  const key = String(estado || '').toUpperCase().replace(/\s+/g, '_');
  return estadoColors[key] || { bg: 'var(--bg-muted)', color: 'var(--text-muted)', label: estado || 'Sin estado' };
};

const EvidenciaDetailModal = ({ evidencia, open, onClose, proyectoId }) => {
  const [versions, setVersions] = useState([]);
  const [loadingVersions, setLoadingVersions] = useState(false);
  const [versionsError, setVersionsError] = useState('');
  const [showVersions, setShowVersions] = useState(false);

  const [viewerBlob, setViewerBlob] = useState(null);
  const [viewerNombre, setViewerNombre] = useState('');
  const [loadingViewer, setLoadingViewer] = useState(false);
  const [viewerError, setViewerError] = useState('');
  const objectUrlRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e) => { if (e.key === 'Escape') onClose(); };
    document.body.classList.add('modal-open');
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.classList.remove('modal-open');
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [open, onClose]);

  useEffect(() => {
    return () => { if (objectUrlRef.current) { URL.revokeObjectURL(objectUrlRef.current); objectUrlRef.current = null; } };
  }, []);

  const fetchVersions = useCallback(async () => {
    if (!evidencia || !proyectoId) return;
    setLoadingVersions(true);
    setVersionsError('');
    setVersions([]);

    try {
      if (evidencia.categoria === 'DOCUMENTO_PROYECTO' && evidencia.tipoDocumento) {
        const response = await documentService.listarVersiones(proyectoId, evidencia.tipoDocumento);
        const v = response?.data?.versiones || response?.versiones || [];
        setVersions(Array.isArray(v) ? v : []);
      } else if (evidencia.categoria === 'EVIDENCIA_ENTREGABLE' && evidencia.entregableId) {
        const response = await projectService.getEntregableVersions(proyectoId, evidencia.entregableId);
        const payload = response?.data ?? response;
        setVersions(Array.isArray(payload) ? payload : []);
      } else {
        setVersions([]);
      }
    } catch (err) {
      console.error('Error fetching versions:', err);
      setVersionsError('No se pudo cargar el historial de versiones.');
    } finally {
      setLoadingVersions(false);
    }
  }, [evidencia, proyectoId]);

  useEffect(() => {
    if (open && evidencia) {
      fetchVersions();
      setShowVersions(false);
    }
  }, [open, evidencia, fetchVersions]);

  const handleViewFile = async () => {
    let url = null;

    if (evidencia?.categoria === 'DOCUMENTO_PROYECTO' && evidencia.tipoDocumento) {
      try {
        setLoadingViewer(true);
        setViewerError('');
        setViewerBlob(null);
        const blob = await documentService.descargarDocumento(proyectoId, evidencia.tipoDocumento);
        if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
        const objectUrl = URL.createObjectURL(blob);
        objectUrlRef.current = objectUrl;
        setViewerBlob(objectUrl);
        setViewerNombre(evidencia.nombreArchivo || evidencia.nombre || 'documento.pdf');
      } catch {
        setViewerError('No fue posible cargar el documento.');
      } finally {
        setLoadingViewer(false);
      }
      return;
    }

    if ((evidencia?.categoria === 'CAMBIO_FECHA' || evidencia?.categoria === 'CAMBIO_DESCRIPCION') && evidencia.cambioId) {
      try {
        setLoadingViewer(true);
        setViewerError('');
        setViewerBlob(null);
        const subPath = evidencia.categoria === 'CAMBIO_DESCRIPCION' ? 'cambios-descripcion' : 'cambios-fecha';
        const path = `/proyectos/${proyectoId}/entregables/${subPath}/${evidencia.cambioId}/descargar`;
        const response = await apiClient.get(path, { responseType: 'blob' });
        if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
        const objectUrl = URL.createObjectURL(response.data);
        objectUrlRef.current = objectUrl;
        setViewerBlob(objectUrl);
        setViewerNombre(evidencia.nombreArchivo || `soporte-cambio-${evidencia.cambioId}.pdf`);
      } catch {
        setViewerError('No fue posible cargar el documento de soporte.');
      } finally {
        setLoadingViewer(false);
      }
      return;
    }

    if (evidencia?.categoria === 'CRONOGRAMA' && evidencia.evidenciaUrl) {
      url = evidencia.evidenciaUrl;
    }

    if (evidencia?.categoria === 'RIESGO' && evidencia.evidenciaUrl) {
      url = evidencia.evidenciaUrl;
    }

    if (evidencia?.categoria === 'EVIDENCIA_ENTREGABLE') {
      url = evidencia.evidenciaUrl;
    }

    if (!url) {
      setViewerError('No hay archivo disponible para previsualizar.');
      return;
    }

    try {
      setLoadingViewer(true);
      setViewerError('');
      setViewerBlob(null);
      const blob = await getEvidenceBlob(url);
      if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
      const objectUrl = URL.createObjectURL(blob);
      objectUrlRef.current = objectUrl;
      setViewerBlob(objectUrl);
      setViewerNombre(evidencia.nombreArchivo || evidencia.nombre || 'evidencia.pdf');
    } catch (err) {
      console.error('Error loading file:', err);
      setViewerError('No fue posible cargar el archivo. Verifica que este disponible.');
    } finally {
      setLoadingViewer(false);
    }
  };

  const handleDownloadFile = async () => {
    if (evidencia?.categoria === 'DOCUMENTO_PROYECTO' && evidencia.tipoDocumento) {
      try {
        const blob = await documentService.descargarDocumento(proyectoId, evidencia.tipoDocumento);
        const blobUrl = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = blobUrl;
        link.setAttribute('download', evidencia.nombreArchivo || 'documento.pdf');
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
        URL.revokeObjectURL(blobUrl);
      } catch (err) { console.error('Error downloading:', err); }
      return;
    }

    if ((evidencia?.categoria === 'CAMBIO_FECHA' || evidencia?.categoria === 'CAMBIO_DESCRIPCION') && evidencia.cambioId) {
      try {
        const subPath = evidencia.categoria === 'CAMBIO_DESCRIPCION' ? 'cambios-descripcion' : 'cambios-fecha';
        const path = `/proyectos/${proyectoId}/entregables/${subPath}/${evidencia.cambioId}/descargar`;
        const response = await apiClient.get(path, { responseType: 'blob' });
        const blobUrl = URL.createObjectURL(response.data);
        const link = document.createElement('a');
        link.href = blobUrl;
        link.setAttribute('download', evidencia.nombreArchivo || `soporte-cambio-${evidencia.cambioId}.pdf`);
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
        URL.revokeObjectURL(blobUrl);
      } catch (err) { console.error('Error downloading:', err); }
      return;
    }

    let url = evidencia?.evidenciaUrl;
    if (!url) return;

    try {
      const blob = await getEvidenceBlob(url);
      const blobUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.setAttribute('download', evidencia.nombreArchivo || evidencia.nombre || 'archivo.pdf');
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      URL.revokeObjectURL(blobUrl);
    } catch (err) { console.error('Error downloading:', err); }
  };

  const handleDownloadVersion = async (version) => {
    try {
      let blob;
      if (evidencia?.categoria === 'DOCUMENTO_PROYECTO' && evidencia.tipoDocumento) {
        blob = await documentService.descargarVersion(proyectoId, evidencia.tipoDocumento, version.numeroVersion);
      } else if (version.descargaUrl) {
        blob = await getEvidenceBlob(version.descargaUrl);
      } else {
        return;
      }
      const blobUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.setAttribute('download', version.nombreArchivo || `version-${version.numeroVersion}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.parentNode.removeChild(link);
      URL.revokeObjectURL(blobUrl);
    } catch (err) { console.error('Error downloading version:', err); }
  };

  const handleViewVersion = async (version) => {
    try {
      setLoadingViewer(true);
      setViewerError('');
      setViewerBlob(null);
      let blob;
      if (evidencia?.categoria === 'DOCUMENTO_PROYECTO' && evidencia.tipoDocumento) {
        blob = await documentService.descargarVersion(proyectoId, evidencia.tipoDocumento, version.numeroVersion);
      } else if (version.descargaUrl) {
        blob = await getEvidenceBlob(version.descargaUrl);
      } else {
        setViewerError('No se pudo cargar esta version.');
        setLoadingViewer(false);
        return;
      }
      if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
      const objectUrl = URL.createObjectURL(blob);
      objectUrlRef.current = objectUrl;
      setViewerBlob(objectUrl);
      setViewerNombre(version.nombreArchivo || `Version ${version.numeroVersion}`);
    } catch {
      setViewerError('No se pudo cargar esta version del archivo.');
    } finally {
      setLoadingViewer(false);
    }
  };

  const handleCloseViewer = () => {
    if (objectUrlRef.current) { URL.revokeObjectURL(objectUrlRef.current); objectUrlRef.current = null; }
    setViewerBlob(null);
    setViewerNombre('');
    setViewerError('');
  };

  if (!open || !evidencia) return null;

  const data = evidencia;
  const estadoInfo = normalizeEstado(data.estadoCodigo || data.estado);
  const nombreArchivo = data.nombreArchivo || data.nombre || 'Documento';
  const fechaRegistro = data.fechaRegistro;
  const usuarioCarga = data.usuario || '';
  const descripcion = data.descripcion || '';
  const entregableNombre = data.entregableNombre || '';
  const hitoNombre = data.hitoNombre || '';
  const faseNombre = data.faseNombre || '';
  const tieneArchivo = data.categoria === 'DOCUMENTO_PROYECTO'
    ? true
    : Boolean(data.evidenciaUrl || data.archivoPdf);
  const isDateChange = data.categoria === 'CAMBIO_FECHA';

  return (
    <div className="edm-overlay" role="presentation" onClick={onClose}>
      <div className="edm-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <header className="edm-header">
          <div className="edm-header-content">
            <span className="edm-kicker">
              {data.categoria === 'DOCUMENTO_PROYECTO' && 'Documento del Proyecto'}
              {data.categoria === 'EVIDENCIA_ENTREGABLE' && 'Evidencia de Entregable'}
              {data.categoria === 'CRONOGRAMA' && 'Cronograma del Proyecto'}
              {data.categoria === 'RIESGO' && 'Solucion de Riesgo'}
              {data.categoria === 'CAMBIO_FECHA' && 'Cambio de Fecha'}
              {data.categoria === 'CAMBIO_DESCRIPCION' && 'Cambio de Descripción'}
            </span>
            <h2>{nombreArchivo}</h2>
            <p>{proyectoId}{entregableNombre ? ` - ${entregableNombre}` : ''}</p>
          </div>
          <button type="button" className="edm-close" onClick={onClose} aria-label="Cerrar detalle">
            <X size={20} />
          </button>
        </header>

        <div className="edm-body">
          <section className="edm-section edm-status-section">
            <div className="edm-status-row">
              <span className="edm-status-label">Estado</span>
              <span className="edm-status-badge" style={{ background: estadoInfo.bg, color: estadoInfo.color }}>
                {estadoInfo.label}
              </span>
            </div>
          </section>

          <section className="edm-section">
            <h3 className="edm-section-title"><Info size={16} /> Informacion General</h3>
            <div className="edm-grid">
              <div className="edm-field">
                <span className="edm-field-label"><FileText size={14} /> Nombre</span>
                <strong className="edm-field-value">{nombreArchivo}</strong>
              </div>
              <div className="edm-field">
                <span className="edm-field-label"><Tag size={14} /> Tipo</span>
                <strong className="edm-field-value">{data.tipo || 'Sin tipo'}</strong>
              </div>
              <div className="edm-field">
                <span className="edm-field-label"><Calendar size={14} /> Fecha</span>
                <strong className="edm-field-value">{formatDate(fechaRegistro) || 'Sin fecha'}</strong>
              </div>
              <div className="edm-field">
                <span className="edm-field-label"><User size={14} /> Usuario</span>
                <strong className="edm-field-value">{usuarioCarga || 'Sin usuario'}</strong>
              </div>
            </div>
          </section>

          {isDateChange && (
            <section className="edm-section">
              <h3 className="edm-section-title"><Clock size={16} /> Cambio de Fecha</h3>
              <div className="edm-grid">
                <div className="edm-field">
                  <span className="edm-field-label">Fecha Anterior</span>
                  <strong className="edm-field-value edm-field-value--danger">{formatDate(data.fechaAnterior) || '--'}</strong>
                </div>
                <div className="edm-field">
                  <span className="edm-field-label">Fecha Nueva</span>
                  <strong className="edm-field-value edm-field-value--success">{formatDate(data.fechaNueva) || '--'}</strong>
                </div>
              </div>
              {data.justificacion && (
                <div className="edm-field" style={{ marginTop: '0.75rem' }}>
                  <span className="edm-field-label"><Info size={14} /> Justificacion</span>
                  <div className="edm-description-box">
                    <p>{data.justificacion}</p>
                  </div>
                </div>
              )}
            </section>
          )}

          {data.categoria === 'CAMBIO_DESCRIPCION' && (
            <section className="edm-section">
              <h3 className="edm-section-title"><FileText size={16} /> Cambio de Descripción</h3>
              <div className="edm-grid" style={{ gridTemplateColumns: '1fr', gap: '0.75rem' }}>
                <div className="edm-field">
                  <span className="edm-field-label">Descripción Anterior</span>
                  <div className="edm-description-box">
                    <p>{data.descripcionAnterior || 'Sin descripción previa'}</p>
                  </div>
                </div>
                <div className="edm-field">
                  <span className="edm-field-label">Descripción Nueva</span>
                  <div className="edm-description-box">
                    <p>{data.descripcionNueva || data.descripcion || '--'}</p>
                  </div>
                </div>
              </div>
              {data.justificacion && (
                <div className="edm-field" style={{ marginTop: '0.75rem' }}>
                  <span className="edm-field-label"><Info size={14} /> Justificación</span>
                  <div className="edm-description-box">
                    <p>{data.justificacion}</p>
                  </div>
                </div>
              )}
            </section>
          )}

          {(faseNombre || hitoNombre || entregableNombre) && !isDateChange && (
            <section className="edm-section">
              <h3 className="edm-section-title"><Tag size={16} /> Jerarquia del Proyecto</h3>
              <div className="edm-grid">
                {faseNombre && (
                  <div className="edm-field">
                    <span className="edm-field-label">Fase</span>
                    <strong className="edm-field-value">{faseNombre}</strong>
                  </div>
                )}
                {hitoNombre && (
                  <div className="edm-field">
                    <span className="edm-field-label">Hito</span>
                    <strong className="edm-field-value">{hitoNombre}</strong>
                  </div>
                )}
                {entregableNombre && (
                  <div className="edm-field">
                    <span className="edm-field-label">Entregable</span>
                    <strong className="edm-field-value">{entregableNombre}</strong>
                  </div>
                )}
              </div>
            </section>
          )}

          {descripcion && !isDateChange && (
            <section className="edm-section">
              <h3 className="edm-section-title"><Info size={16} /> Descripcion</h3>
              <div className="edm-description-box"><p>{descripcion}</p></div>
            </section>
          )}

          {versions.length > 0 && (
            <section className="edm-section">
              <button type="button" className="edm-section-toggle" onClick={() => setShowVersions(!showVersions)}>
                <h3 className="edm-section-title"><History size={16} /> Historial de Versiones ({versions.length})</h3>
                {showVersions ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>
              {showVersions && (
                <div className="edm-versions-list">
                  {versions.map((v) => {
                    const isActual = v.actual || v.estado === 'ACTUAL';
                    return (
                      <div key={v.id || v.numeroVersion} className={`edm-version-item ${isActual ? 'edm-version-item--actual' : ''}`}>
                        <div className="edm-version-info">
                          <div className="edm-version-header-row">
                            <span className="edm-version-number">v{v.numeroVersion}</span>
                            {isActual && <span className="edm-version-badge">Actual</span>}
                            <span className="edm-version-date">{v.subidoEn}</span>
                          </div>
                          <span className="edm-version-filename">{v.nombreArchivo}</span>
                          {v.observacion && <span className="edm-version-observation">"{v.observacion}"</span>}
                          <span className="edm-version-meta">
                            {v.subidoPor}{v.subidoRol ? ` - ${v.subidoRol}` : ''}{v.tamanoFormateado ? ` - ${v.tamanoFormateado}` : ''}
                          </span>
                        </div>
                        <div className="edm-version-actions">
                          <button type="button" className="evp-btn-view" onClick={() => handleViewVersion(v)} title="Ver version">
                            <Eye size={14} />
                          </button>
                          <button type="button" className="evp-btn-view" onClick={() => handleDownloadVersion(v)} title="Descargar version">
                            <Download size={14} />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          )}

          {loadingVersions && (
            <div className="edm-loading-inline">
              <LoaderCircle size={16} className="animate-spin" /> Cargando historial...
            </div>
          )}
          {versionsError && (
            <div className="edm-error-inline">
              <AlertCircle size={14} /> {versionsError}
            </div>
          )}
        </div>

        <footer className="edm-footer">
          <button type="button" className="edm-btn edm-btn-secondary" onClick={onClose}>Cerrar</button>
          {tieneArchivo && (
            <button type="button" className="edm-btn edm-btn-secondary" onClick={handleDownloadFile}>
              <Download size={16} /> Descargar
            </button>
          )}
          <button type="button" className="edm-btn edm-btn-primary" onClick={handleViewFile} disabled={loadingViewer || !tieneArchivo}>
            {loadingViewer ? <LoaderCircle size={16} className="animate-spin" /> : <Eye size={16} />}
            {loadingViewer ? 'Cargando...' : 'Ver Archivo'}
          </button>
        </footer>
      </div>

      {(viewerError || viewerBlob) && (
        <div className="edm-viewer-overlay" role="presentation" onClick={handleCloseViewer}>
          <div className="edm-viewer-modal" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <header className="edm-viewer-header">
              <div className="edm-viewer-title">
                {viewerError ? <AlertCircle size={16} /> : <FileText size={16} />}
                <span>{viewerError ? 'Error' : viewerNombre}</span>
              </div>
              <button type="button" className="edm-viewer-close" onClick={handleCloseViewer} aria-label="Cerrar">
                <X size={18} />
              </button>
            </header>
            <div className="edm-viewer-body">
              {viewerError ? (
                <div className="edm-viewer-fallback"><FileText size={48} strokeWidth={1} /><p>{viewerError}</p></div>
              ) : (
                <object data={viewerBlob} type="application/pdf" className="edm-viewer-iframe">
                  <p className="edm-viewer-fallback-text">No se pudo previsualizar. <a href={viewerBlob} target="_blank" rel="noreferrer">Abrir en nueva pestana</a></p>
                </object>
              )}
            </div>
            <div className="edm-viewer-footer">
              <button type="button" className="edm-btn edm-btn-secondary" onClick={handleCloseViewer}>Cerrar</button>
              <button type="button" className="edm-btn edm-btn-primary" onClick={handleDownloadFile}>
                <Download size={16} /> Descargar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default EvidenciaDetailModal;
