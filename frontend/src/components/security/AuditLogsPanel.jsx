import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Eye,
  LoaderCircle,
  RefreshCw,
  Search,
  SearchX,
} from 'lucide-react';
import auditService from '../../services/auditService';
import AuditLogDetailDrawer from './AuditLogDetailDrawer';
import './AuditLogsPanel.css';

const formatDateTime = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

const accionMeta = {
  CONSULTA: { label: 'Consulta' },
  CREACION: { label: 'Creación' },
  ACTUALIZACION: { label: 'Actualización' },
  ELIMINACION: { label: 'Eliminación' },
  LOGIN: { label: 'Login' },
  OTRO: { label: 'Otro' },
};

const estadoMeta = {
  SUCCESS: { label: 'Éxito' },
  ERROR: { label: 'Error' },
};

const httpMethodMeta = {
  GET: 'GET',
  POST: 'POST',
  PUT: 'PUT',
  PATCH: 'PATCH',
  DELETE: 'DELETE',
};

const extractApiDetail = (error) => {
  const data = error?.response?.data;
  const status = error?.response?.status;
  const detail = data?.detail || data?.message || data?.error;
  if (detail) return detail;
  if (error?.message) return error.message;
  if (status) return `Error del servidor (HTTP ${status})`;
  return 'Error desconocido. Revisa la consola del navegador para más detalles.';
};

const AuditLogsPanel = () => {
  const [entries, setEntries] = useState([]);
  const [stats, setStats] = useState({ total: 0, exitosos: 0, errores: 0, porAccion: {}, porCodigoEstado: {} });
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedLog, setSelectedLog] = useState(null);

  const [accionFilter, setAccionFilter] = useState('ALL');
  const [estadoFilter, setEstadoFilter] = useState('ALL');
  const [metodoFilter, setMetodoFilter] = useState('ALL');
  const [usuarioFilter, setUsuarioFilter] = useState('');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  const pageRef = useRef(0);
  const pageSize = 20;

  const loadData = useCallback(async (targetPage, accion, estado, metodo, usuario, searchTerm) => {
    try {
      setLoading(true);
      setError('');
      const params = {
        accion: accion !== 'ALL' ? accion : undefined,
        estado: estado !== 'ALL' ? estado : undefined,
        metodoHttp: metodo !== 'ALL' ? metodo : undefined,
        usuarioId: usuario?.trim() || undefined,
        search: searchTerm?.trim() || undefined,
        page: targetPage,
        size: pageSize,
      };
      const result = await auditService.listLogs(params);
      const data = result?.data?.data || result?.data || result || {};
      setEntries(data.entries || []);
      setTotalPages(data.totalPages || 0);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('[AuditLogsPanel] Error al listar auditoría:', err);
      setError(`Error al cargar auditoría: ${extractApiDetail(err)}`);
      setEntries([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadStats = useCallback(async () => {
    try {
      const result = await auditService.getStats();
      const data = result?.data?.data || result?.data || result || {};
      setStats(data || {});
    } catch {
      // stats son secundarias; no bloqueamos el panel
    }
  }, []);

  const refresh = useCallback(async () => {
    pageRef.current = 0;
    setPage(0);
    await loadData(0, accionFilter, estadoFilter, metodoFilter, usuarioFilter, debouncedSearch);
    await loadStats();
  }, [loadData, loadStats, accionFilter, estadoFilter, metodoFilter, usuarioFilter, debouncedSearch]);

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 350);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    pageRef.current = 0;
    setPage(0);
    loadData(0, accionFilter, estadoFilter, metodoFilter, usuarioFilter, debouncedSearch);
  }, [accionFilter, estadoFilter, metodoFilter, debouncedSearch, loadData]);

  const handlePageChange = (nextPage) => {
    pageRef.current = nextPage;
    setPage(nextPage);
    loadData(nextPage, accionFilter, estadoFilter, metodoFilter, usuarioFilter, debouncedSearch);
  };

  const handleView = async (log) => {
    try {
      const result = await auditService.getLogDetail(log.id);
      const data = result?.data?.data || result?.data || result;
      setSelectedLog(data || log);
    } catch (err) {
      console.error('[AuditLogsPanel] Error al cargar detalle:', err);
      setError(`Error al cargar el detalle: ${extractApiDetail(err)}`);
      setSelectedLog(log);
    }
  };

  return (
    <>
      <section className="audit-panel panel panel-main">
        <div className="panel-topbar audit-topbar">
          <div className="roles-header-copy">
            <p className="security-eyebrow">AUDITORÍA DE LOGS</p>
            <h2>Registro de actividad del sistema</h2>
            <p>
              Trazabilidad de todas las peticiones HTTP y acciones ejecutadas por los usuarios.
            </p>
          </div>
          <button type="button" className="btn-secondary" onClick={refresh} disabled={loading}>
            <RefreshCw size={16} />
            Actualizar
          </button>
        </div>

        <div className="audit-stats">
          <div className="audit-stat-card">
            <div className="audit-stat-icon total"><Activity size={18} /></div>
            <div>
              <span className="audit-stat-value">{stats.total || 0}</span>
              <span className="audit-stat-label">Total peticiones</span>
            </div>
          </div>
          <div className="audit-stat-card">
            <div className="audit-stat-icon ok"><CheckCircle2 size={18} /></div>
            <div>
              <span className="audit-stat-value">{stats.exitosos || 0}</span>
              <span className="audit-stat-label">Exitosas</span>
            </div>
          </div>
          <div className="audit-stat-card">
            <div className="audit-stat-icon error"><AlertTriangle size={18} /></div>
            <div>
              <span className="audit-stat-value">{stats.errores || 0}</span>
              <span className="audit-stat-label">Con error</span>
            </div>
          </div>
        </div>

        <div className="audit-filters">
          <div className="audit-filter-field audit-search">
            <Search size={15} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar por módulo, recurso o detalle..."
            />
          </div>
          <div className="audit-filter-field">
            <label>Acción</label>
            <select value={accionFilter} onChange={(e) => setAccionFilter(e.target.value)}>
              <option value="ALL">Todas</option>
              {Object.entries(accionMeta).map(([key, meta]) => (
                <option key={key} value={key}>{meta.label}</option>
              ))}
            </select>
          </div>
          <div className="audit-filter-field">
            <label>Estado</label>
            <select value={estadoFilter} onChange={(e) => setEstadoFilter(e.target.value)}>
              <option value="ALL">Todos</option>
              {Object.entries(estadoMeta).map(([key, meta]) => (
                <option key={key} value={key}>{meta.label}</option>
              ))}
            </select>
          </div>
          <div className="audit-filter-field">
            <label>Método</label>
            <select value={metodoFilter} onChange={(e) => setMetodoFilter(e.target.value)}>
              <option value="ALL">Todos</option>
              {Object.entries(httpMethodMeta).map(([key, label]) => (
                <option key={key} value={key}>{label}</option>
              ))}
            </select>
          </div>
          <div className="audit-filter-field audit-search">
            <Search size={15} />
            <input
              type="text"
              value={usuarioFilter}
              onChange={(e) => setUsuarioFilter(e.target.value)}
              placeholder="Usuario..."
            />
          </div>
        </div>

        {error && (
          <div className="feedback-banner error">
            <AlertTriangle size={18} />
            <span>{error}</span>
          </div>
        )}

        <div className="table-shell audit-table-shell">
          {loading ? (
            <div className="audit-loading">
              <LoaderCircle className="spinning" size={22} />
              <span>Cargando registros...</span>
            </div>
          ) : entries.length === 0 ? (
            <div className="audit-empty">
              <SearchX size={32} />
              <span>No se encontraron registros de auditoría.</span>
            </div>
          ) : (
            <table className="data-table audit-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Usuario</th>
                  <th>Acción</th>
                  <th>Módulo</th>
                  <th>Método</th>
                  <th>Código</th>
                  <th>Estado</th>
                  <th>Duración</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => {
                  const accion = accionMeta[entry.accion] || { label: entry.accion };
                  const estado = estadoMeta[entry.estado] || { label: entry.estado };
                  return (
                    <tr key={entry.id}>
                      <td className="audit-date">{formatDateTime(entry.fechaCreacion)}</td>
                      <td>
                        <div className="audit-user">
                          <span className="audit-user-name">{entry.usuarioNombre || entry.usuarioId || 'SYSTEM'}</span>
                          {entry.usuarioRol && (
                            <span className="audit-user-role">{entry.usuarioRol}</span>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className={`audit-action-badge ${String(entry.accion || '').toLowerCase()}`}>
                          {accion.label}
                        </span>
                      </td>
                      <td className="audit-modulo" title={entry.modulo}>{entry.modulo || '-'}</td>
                      <td>
                        <span className={`audit-method ${String(entry.metodoHttp || '').toLowerCase()}`}>
                          {entry.metodoHttp || '-'}
                        </span>
                      </td>
                      <td>
                        {entry.codigoEstado != null ? (
                          <span
                            className={`audit-http-badge ${entry.codigoEstado < 300 ? 'ok' : entry.codigoEstado < 400 ? 'redir' : entry.codigoEstado < 500 ? 'client' : 'server'}`}
                          >
                            {entry.codigoEstado}
                          </span>
                        ) : '-'}
                      </td>
                      <td>
                        <span className={`audit-state-badge ${String(entry.estado || '').toLowerCase()}`}>
                          {estado.label}
                        </span>
                      </td>
                      <td className="audit-duration">
                        {entry.duracionMs != null ? `${entry.duracionMs} ms` : '-'}
                      </td>
                      <td>
                        <button
                          type="button"
                          className="audit-view-btn"
                          onClick={() => handleView(entry)}
                          title="Ver detalle"
                        >
                          <Eye size={16} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        {totalPages > 1 && (
          <footer className="audit-footer">
            <span className="audit-page-info">
              {page + 1} de {totalPages} páginas ({totalElements} registros)
            </span>
            <div className="audit-pagination">
              <button
                type="button"
                className="pagination-btn"
                onClick={() => handlePageChange(Math.max(0, page - 1))}
                disabled={page === 0}
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                let pageNum;
                if (totalPages <= 5) {
                  pageNum = i;
                } else if (page < 3) {
                  pageNum = i;
                } else if (page >= totalPages - 3) {
                  pageNum = totalPages - 5 + i;
                } else {
                  pageNum = page - 2 + i;
                }
                return (
                  <button
                    key={pageNum}
                    type="button"
                    className={`pagination-btn ${pageNum === page ? 'active' : ''}`}
                    onClick={() => handlePageChange(pageNum)}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
              <button
                type="button"
                className="pagination-btn"
                onClick={() => handlePageChange(Math.min(totalPages - 1, page + 1))}
                disabled={page >= totalPages - 1}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </footer>
        )}
      </section>

      {selectedLog && (
        <AuditLogDetailDrawer
          log={selectedLog}
          onClose={() => setSelectedLog(null)}
        />
      )}
    </>
  );
};

export default AuditLogsPanel;
