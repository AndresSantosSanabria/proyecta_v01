import { useCallback, useEffect, useState } from 'react';
import {
  Search, Upload, Download, Eye, FileText, Calendar, User, Tag,
  AlertCircle, LoaderCircle, FolderOpen, X, RefreshCw,
} from 'lucide-react';
import internalDocService from '../../services/internalDocService';
import { usePermission } from '../../hooks/usePermission';
import { formatDate } from '../../utils/locale';
import { resolveLoadErrorMessage } from '../../utils/accessMessages';
import './DocumentacionInternaPage.css';

const MAX_SIZE_MB = 20;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;
const PAGE_SIZE = 20;

const DocumentacionInternaPage = () => {
  const canUpload = usePermission('DOCUMENTO_INTERNO:CARGAR');

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [documentos, setDocumentos] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [knownYears, setKnownYears] = useState([]);

  const [filterNombre, setFilterNombre] = useState('');
  const [filterAnio, setFilterAnio] = useState('');
  const [filterDescripcion, setFilterDescripcion] = useState('');

  const [showUpload, setShowUpload] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [form, setForm] = useState({
    nombre: '',
    descripcion: '',
    fechaCreacion: '',
    file: null,
  });

  const [viewer, setViewer] = useState(null);

  const closeViewer = () => {
    setViewer((current) => {
      if (current?.url) {
        try { URL.revokeObjectURL(current.url); } catch { /* noop */ }
      }
      return null;
    });
  };

  const fetchDocumentos = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {
        nombre: filterNombre.trim() || undefined,
        anio: filterAnio ? Number(filterAnio) : undefined,
        descripcion: filterDescripcion.trim() || undefined,
        page,
        size: PAGE_SIZE,
      };
      const result = await internalDocService.listar(params);
      const docs = Array.isArray(result?.documentos) ? result.documentos : [];
      setDocumentos(docs);
      setTotal(Number(result?.total ?? docs.length));
      if (!filterNombre && !filterAnio && !filterDescripcion) {
        setKnownYears((prev) => {
          const set = new Set(prev);
          docs.forEach((d) => {
            if (d.fechaCreacion) set.add(String(d.fechaCreacion).slice(0, 4));
          });
          const next = Array.from(set).sort((a, b) => b.localeCompare(a));
          if (next.length === prev.length && next.every((y, i) => y === prev[i])) {
            return prev;
          }
          return next;
        });
      }
    } catch (err) {
      setError(resolveLoadErrorMessage(err, 'No se pudo cargar la documentacion interna.'));
    } finally {
      setLoading(false);
    }
  }, [filterNombre, filterAnio, filterDescripcion, page]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchDocumentos();
    }, 300);
    return () => clearTimeout(timer);
  }, [fetchDocumentos]);

  const anios = knownYears;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const resetFilters = () => {
    setFilterNombre('');
    setFilterAnio('');
    setFilterDescripcion('');
    setPage(0);
  };

  const handleFileChange = (e) => {
    const file = e.target.files?.[0] || null;
    if (file && file.size > MAX_SIZE_BYTES) {
      setError(`El archivo excede el tamano maximo de ${MAX_SIZE_MB} MB.`);
      e.target.value = '';
      return;
    }
    if (file && file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      setError('Solo se permiten archivos PDF.');
      e.target.value = '';
      return;
    }
    setForm((p) => ({ ...p, file }));
    setError(null);
  };

  const handleFilterChange = (setter) => (e) => {
    setter(e.target.value);
    setPage(0);
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) {
      setError('El nombre del documento es obligatorio.');
      return;
    }
    if (!form.file) {
      setError('Debe seleccionar un archivo.');
      return;
    }
    try {
      setUploading(true);
      setError(null);
      setSuccess(null);
      setUploadProgress(0);
      await internalDocService.cargar({
        nombre: form.nombre.trim(),
        descripcion: form.descripcion.trim() || undefined,
        fechaCreacion: form.fechaCreacion || undefined,
        file: form.file,
        onUploadProgress: setUploadProgress,
      });
      setSuccess('Documento cargado exitosamente.');
      setForm({ nombre: '', descripcion: '', fechaCreacion: '', file: null });
      setShowUpload(false);
      await fetchDocumentos();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || 'No se pudo cargar el documento.');
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  const handleDownload = async (doc) => {
    try {
      const blob = await internalDocService.descargar(doc.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.nombreOriginal || `${doc.codigo}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(err?.response?.data?.message || 'No se pudo descargar el documento.');
    }
  };

  const handleView = async (doc) => {
    try {
      setError(null);
      const url = await internalDocService.verComoBlobUrl(doc.id);
      setViewer({ doc, url });
    } catch (err) {
      setError(err?.response?.data?.message || 'No se pudo abrir el documento.');
    }
  };

  const isPdf = (doc) => (doc.mimeType || '').toLowerCase().includes('pdf') || (doc.nombreOriginal || '').toLowerCase().endsWith('.pdf');

  return (
    <div className="di-page">
      <div className="di-topbar">
        <div className="di-brand">
          <p className="di-eyebrow">MODULO</p>
          <h1>Documentacion Interna</h1>
          <p className="di-subtitle">Archivos internos de referencia para gestores y administracion.</p>
        </div>
        <div className="di-topbar-actions">
          <button type="button" className="di-ghost-btn" onClick={fetchDocumentos} title="Actualizar">
            <RefreshCw size={16} />
            Actualizar
          </button>
          {canUpload && (
            <button type="button" className="di-primary-btn" onClick={() => setShowUpload((v) => !v)}>
              <Upload size={16} />
              Cargar documento
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="di-alert di-alert--error">
          <AlertCircle size={18} />
          <span>{error}</span>
          <button type="button" onClick={() => setError(null)} aria-label="Cerrar"><X size={16} /></button>
        </div>
      )}
      {success && (
        <div className="di-alert di-alert--success">
          <span>{success}</span>
          <button type="button" onClick={() => setSuccess(null)} aria-label="Cerrar"><X size={16} /></button>
        </div>
      )}

      {showUpload && canUpload && (
        <form className="di-upload-panel" onSubmit={handleUpload}>
          <div className="di-upload-panel__header">
            <h2>Nuevo documento interno</h2>
            <p>El codigo <strong>DOC-ANIO-MES-DIA-HH:MM</strong> se genera automaticamente.</p>
          </div>
          <div className="di-upload-grid">
            <label className="di-field">
              <span>Nombre *</span>
              <input
                type="text"
                value={form.nombre}
                onChange={(e) => setForm((p) => ({ ...p, nombre: e.target.value }))}
                placeholder="Nombre del documento"
                required
                disabled={uploading}
              />
            </label>
            <label className="di-field">
              <span>Fecha de creacion</span>
              <input
                type="date"
                value={form.fechaCreacion}
                onChange={(e) => setForm((p) => ({ ...p, fechaCreacion: e.target.value }))}
                disabled={uploading}
              />
            </label>
            <label className="di-field di-field--full">
              <span>Descripcion</span>
              <textarea
                value={form.descripcion}
                onChange={(e) => setForm((p) => ({ ...p, descripcion: e.target.value }))}
                placeholder="Descripcion o proposito del documento"
                rows={3}
                disabled={uploading}
              />
            </label>
            <label className="di-field di-field--full">
              <span>Archivo * (solo PDF — max {MAX_SIZE_MB} MB)</span>
              <input type="file" onChange={handleFileChange} accept=".pdf,application/pdf" disabled={uploading} required />
            </label>
          </div>
          {uploading && (
            <div className="di-progress">
              <div className="di-progress__bar" style={{ width: `${uploadProgress}%` }} />
              <span>{uploadProgress}%</span>
            </div>
          )}
          <div className="di-upload-actions">
            <button type="button" className="di-ghost-btn" onClick={() => setShowUpload(false)} disabled={uploading}>
              Cancelar
            </button>
            <button type="submit" className="di-primary-btn" disabled={uploading}>
              {uploading ? <LoaderCircle size={16} className="di-spin" /> : <Upload size={16} />}
              {uploading ? 'Cargando...' : 'Guardar'}
            </button>
          </div>
        </form>
      )}

      <div className="di-filters">
        <div className="di-search-box">
          <Search size={18} />
          <input
            type="text"
            value={filterNombre}
            onChange={(e) => setFilterNombre(e.target.value)}
            placeholder="Buscar por nombre..."
          />
        </div>
        <label className="di-filter-field">
          <Calendar size={16} />
          <select value={filterAnio} onChange={(e) => setFilterAnio(e.target.value)}>
            <option value="">Todos los años</option>
            {anios.map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
          </select>
        </label>
        <div className="di-search-box di-search-box--desc">
          <Tag size={18} />
          <input
            type="text"
            value={filterDescripcion}
            onChange={(e) => setFilterDescripcion(e.target.value)}
            placeholder="Buscar por descripcion..."
          />
        </div>
        {(filterNombre || filterAnio || filterDescripcion) && (
          <button type="button" className="di-ghost-btn" onClick={resetFilters}>
            <X size={16} />
            Limpiar
          </button>
        )}
      </div>

      {loading ? (
        <div className="di-empty">
          <LoaderCircle size={32} className="di-spin" />
          <p>Cargando documentos...</p>
        </div>
      ) : documentos.length === 0 ? (
        <div className="di-empty">
          <FolderOpen size={40} />
          <p>No se encontraron documentos internos.</p>
        </div>
      ) : (
        <div className="di-table-wrap">
          <table className="di-table">
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Nombre</th>
                <th>Descripcion</th>
                <th>Fecha creacion</th>
                <th>Tamano</th>
                <th>Cargado por</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {documentos.map((doc) => (
                <tr key={doc.id}>
                  <td><span className="di-code">{doc.codigo}</span></td>
                  <td>
                    <div className="di-cell-name">
                      <FileText size={16} />
                      <span title={doc.nombre}>{doc.nombre}</span>
                    </div>
                  </td>
                  <td className="di-muted" title={doc.descripcion || ''}>
                    {doc.descripcion ? (doc.descripcion.length > 60 ? `${doc.descripcion.slice(0, 60)}...` : doc.descripcion) : '—'}
                  </td>
                  <td>{doc.fechaCreacion ? formatDate(doc.fechaCreacion) : '—'}</td>
                  <td>{doc.tamanoFormateado || '—'}</td>
                  <td>
                    <div className="di-cell-user">
                      <User size={14} />
                      <span>{doc.creadoPor || '—'}</span>
                    </div>
                  </td>
                  <td>
                    <div className="di-actions">
                      {isPdf(doc) && (
                        <button type="button" className="di-icon-btn" title="Ver documento" onClick={() => handleView(doc)}>
                          <Eye size={16} />
                        </button>
                      )}
                      <button type="button" className="di-icon-btn" title="Descargar" onClick={() => handleDownload(doc)}>
                        <Download size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && total > PAGE_SIZE && (
        <div className="di-pagination">
          <button
            type="button"
            className="di-ghost-btn"
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Anterior
          </button>
          <span className="di-pagination__info">
            Pagina {page + 1} de {totalPages} · {total} documentos
          </span>
          <button
            type="button"
            className="di-ghost-btn"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            Siguiente
          </button>
        </div>
      )}

      {viewer && (
        <div className="di-modal-backdrop" onClick={closeViewer}>
          <div className="di-modal" onClick={(e) => e.stopPropagation()}>
            <div className="di-modal__header">
              <div>
                <span className="di-code">{viewer.doc.codigo}</span>
                <h3>{viewer.doc.nombre}</h3>
              </div>
              <div className="di-modal__actions">
                <button type="button" className="di-ghost-btn" onClick={() => handleDownload(viewer.doc)}>
                  <Download size={16} />
                  Descargar
                </button>
                <button type="button" className="di-icon-btn" onClick={closeViewer} aria-label="Cerrar">
                  <X size={18} />
                </button>
              </div>
            </div>
            <iframe
              className="di-pdf-frame"
              title={viewer.doc.nombre}
              src={viewer.url}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentacionInternaPage;
