import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  RefreshCw,
  Search,
  X,
  AlertTriangle,
  Mail,
  Bell,
} from 'lucide-react';
import securityService from '../../services/securityService';

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

const channelMeta = {
  EMAIL: { label: 'EMAIL', icon: Mail, className: 'channel-email' },
  IN_APP: { label: 'IN-APP', icon: Bell, className: 'channel-in-app' },
};

const extractApiDetail = (error) =>
  error?.response?.data?.message || error?.response?.data?.detail || error?.message || '';

const NotificationFailuresModal = ({ isOpen, onClose }) => {
  const [activeTab, setActiveTab] = useState('audit');
  const [auditEntries, setAuditEntries] = useState([]);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [auditTotalElements, setAuditTotalElements] = useState(0);
  const [auditPage, setAuditPage] = useState(0);
  const [dispatchEntries, setDispatchEntries] = useState([]);
  const [dispatchTotalPages, setDispatchTotalPages] = useState(0);
  const [dispatchTotalElements, setDispatchTotalElements] = useState(0);
  const [dispatchPage, setDispatchPage] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [channelFilter, setChannelFilter] = useState('ALL');
  const [recipientFilter, setRecipientFilter] = useState('');
  const [debouncedRecipient, setDebouncedRecipient] = useState('');

  const auditPageRef = useRef(0);
  const dispatchPageRef = useRef(0);

  const pageSize = 10;

  const loadAuditData = useCallback(async (page, channel, recipient) => {
    try {
      setLoading(true);
      setError('');
      const params = {
        page,
        size: pageSize,
        recipient: recipient || undefined,
        channel: channel !== 'ALL' ? channel : undefined,
      };
      const result = await securityService.getFailedNotifications(params);
      const data = result?.data?.data || result?.data || result || {};
      setAuditEntries(data.entries || []);
      setAuditTotalPages(data.totalPages || 0);
      setAuditTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(`Error al cargar auditoría: ${extractApiDetail(err)}`);
      setAuditEntries([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadDispatchData = useCallback(async (page, recipient) => {
    try {
      setLoading(true);
      setError('');
      const params = {
        page,
        size: pageSize,
        recipient: recipient || undefined,
      };
      const result = await securityService.getFailedDispatchLogs(params);
      const data = result?.data?.data || result?.data || result || {};
      setDispatchEntries(data.entries || []);
      setDispatchTotalPages(data.totalPages || 0);
      setDispatchTotalElements(data.totalElements || 0);
    } catch (err) {
      setError(`Error al cargar dispatch: ${extractApiDetail(err)}`);
      setDispatchEntries([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleEsc);
    document.body.classList.add('modal-open');
    return () => {
      document.removeEventListener('keydown', handleEsc);
      document.body.classList.remove('modal-open');
    };
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!isOpen) return;
    const timer = setTimeout(() => setDebouncedRecipient(recipientFilter), 300);
    return () => clearTimeout(timer);
  }, [recipientFilter, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    const targetPage = 0;
    if (activeTab === 'audit') {
      auditPageRef.current = targetPage;
      setAuditPage(targetPage);
    } else {
      dispatchPageRef.current = targetPage;
      setDispatchPage(targetPage);
    }
  }, [activeTab, channelFilter, debouncedRecipient, isOpen]);

  useEffect(() => {
    if (!isOpen) return;
    if (activeTab === 'audit') {
      loadAuditData(auditPage, channelFilter, debouncedRecipient);
    } else {
      loadDispatchData(dispatchPage, debouncedRecipient);
    }
  }, [activeTab, auditPage, dispatchPage, channelFilter, debouncedRecipient, isOpen, loadAuditData, loadDispatchData]);

  const handleRefresh = () => {
    if (activeTab === 'audit') {
      loadAuditData(auditPage, channelFilter, debouncedRecipient);
    } else {
      loadDispatchData(dispatchPage, debouncedRecipient);
    }
  };

  const handlePageChange = (newPage) => {
    if (activeTab === 'audit') {
      auditPageRef.current = newPage;
      setAuditPage(newPage);
    } else {
      dispatchPageRef.current = newPage;
      setDispatchPage(newPage);
    }
  };

  if (!isOpen) return null;

  const totalPages = activeTab === 'audit' ? auditTotalPages : dispatchTotalPages;
  const totalElements = activeTab === 'audit' ? auditTotalElements : dispatchTotalElements;
  const currentPage = activeTab === 'audit' ? auditPage : dispatchPage;

  return (
    <div className="failures-overlay" role="presentation" onMouseDown={onClose}>
      <article
        className="failures-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="failures-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <header className="failures-modal__header">
          <div className="failures-modal__title">
            <button type="button" className="failures-modal__back" onClick={onClose} aria-label="Cerrar">
              <AlertTriangle size={18} />
            </button>
            <div>
              <p className="security-eyebrow">NOTIFICACIONES FALLIDAS</p>
              <h3 id="failures-title">Detalle de errores</h3>
            </div>
          </div>
          <button type="button" className="failures-modal__close" onClick={onClose} aria-label="Cerrar">
            <X size={18} />
          </button>
        </header>

        <div className="failures-modal__tabs">
          <button
            type="button"
            className={`failures-tab ${activeTab === 'audit' ? 'active' : ''}`}
            onClick={() => setActiveTab('audit')}
          >
            Auditoría
            <span className="failures-tab__count">{auditTotalElements}</span>
          </button>
          <button
            type="button"
            className={`failures-tab ${activeTab === 'dispatch' ? 'active' : ''}`}
            onClick={() => setActiveTab('dispatch')}
          >
            Dispatch detallado
            <span className="failures-tab__count">{dispatchTotalElements}</span>
          </button>
        </div>

        <div className="failures-modal__toolbar">
          <div className="failures-filters">
            {activeTab === 'audit' && (
              <select
                className="failures-select"
                value={channelFilter}
                onChange={(e) => setChannelFilter(e.target.value)}
              >
                <option value="ALL">Todos los canales</option>
                <option value="EMAIL">Email</option>
                <option value="IN_APP">In-App</option>
              </select>
            )}
            <div className="inline-search">
              <Search size={16} />
              <input
                type="text"
                value={recipientFilter}
                onChange={(e) => setRecipientFilter(e.target.value)}
                placeholder="Buscar por destinatario..."
              />
            </div>
          </div>
          <button type="button" className="btn-secondary" onClick={handleRefresh} disabled={loading}>
            {loading ? <LoaderCircle size={14} className="animate-spin" /> : <RefreshCw size={14} />}
            Actualizar
          </button>
        </div>

        <div className="failures-modal__body">
          {error && <div className="feedback-banner error">{error}</div>}

          {loading ? (
            <div className="failures-loading">
              <LoaderCircle size={24} className="animate-spin" />
              <span>Cargando registros...</span>
            </div>
          ) : activeTab === 'audit' ? (
            auditEntries.length === 0 ? (
              <div className="failures-empty">
                <AlertTriangle size={32} />
                <span>No se encontraron notificaciones fallidas.</span>
              </div>
            ) : (
              <div className="failures-table-wrap">
                <table className="data-table failures-table">
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Evento</th>
                      <th>Destinatario</th>
                      <th>Canal</th>
                      <th>Razón del fallo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditEntries.map((entry) => {
                      const ch = channelMeta[entry.channel] || { label: entry.channel, className: '' };
                      return (
                        <tr key={entry.id}>
                          <td className="failures-date">{formatDateTime(entry.createdAt)}</td>
                          <td>
                            <span className="failures-event-code">{entry.eventCode}</span>
                          </td>
                          <td className="failures-recipient">{entry.recipient}</td>
                          <td>
                            <span className={`failures-channel ${ch.className}`}>{ch.label}</span>
                          </td>
                          <td className="failures-reason" title={entry.failureReason}>
                            {entry.failureReason || '-'}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )
          ) : dispatchEntries.length === 0 ? (
            <div className="failures-empty">
              <AlertTriangle size={32} />
              <span>No se encontraron notificaciones fallidas.</span>
            </div>
          ) : (
            <div className="failures-table-wrap">
              <table className="data-table failures-table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Asunto</th>
                    <th>Destinatario</th>
                    <th>Estado</th>
                    <th>Detalle</th>
                  </tr>
                </thead>
                <tbody>
                  {dispatchEntries.map((entry) => (
                    <tr key={entry.id}>
                      <td className="failures-date">{formatDateTime(entry.createdAt)}</td>
                      <td className="failures-subject">{entry.subject}</td>
                      <td className="failures-recipient">{entry.recipient}</td>
                      <td>
                        <span className="failures-status-badge">{entry.status}</span>
                      </td>
                      <td className="failures-reason" title={entry.detail}>
                        {entry.detail || '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {totalPages > 1 && (
          <footer className="failures-modal__footer">
            <span className="failures-page-info">
              {currentPage + 1} de {totalPages} páginas ({totalElements} registros)
            </span>
            <div className="failures-pagination">
              <button
                type="button"
                className="pagination-btn"
                onClick={() => handlePageChange(Math.max(0, currentPage - 1))}
                disabled={currentPage === 0}
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                let pageNum;
                if (totalPages <= 5) {
                  pageNum = i;
                } else if (currentPage < 3) {
                  pageNum = i;
                } else if (currentPage >= totalPages - 3) {
                  pageNum = totalPages - 5 + i;
                } else {
                  pageNum = currentPage - 2 + i;
                }
                return (
                  <button
                    key={pageNum}
                    type="button"
                    className={`pagination-btn ${pageNum === currentPage ? 'active' : ''}`}
                    onClick={() => handlePageChange(pageNum)}
                  >
                    {pageNum + 1}
                  </button>
                );
              })}
              <button
                type="button"
                className="pagination-btn"
                onClick={() => handlePageChange(Math.min(totalPages - 1, currentPage + 1))}
                disabled={currentPage >= totalPages - 1}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </footer>
        )}
      </article>
    </div>
  );
};

export default NotificationFailuresModal;
