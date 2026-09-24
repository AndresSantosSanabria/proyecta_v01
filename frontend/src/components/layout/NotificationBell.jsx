import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BellRing, CheckCheck, Clock3, LoaderCircle, X, FolderKanban, Filter, AlertTriangle, FileCheck, Send, RotateCcw, Mail, Eye, Upload, ClipboardList } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import securityService from '../../services/securityService';
import { formatDateTime } from '../../utils/locale';
import './NotificationBell.css';

const compactTime = (value) => {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const now = new Date();
  const sameDay =
    date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday =
    date.getFullYear() === yesterday.getFullYear()
    && date.getMonth() === yesterday.getMonth()
    && date.getDate() === yesterday.getDate();
  const time = formatDateTime(value, { hour: '2-digit', minute: '2-digit', hour12: false });
  if (sameDay) return `Hoy ${time}`;
  if (isYesterday) return `Ayer ${time}`;
  return formatDateTime(value, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', hour12: false });
};

const extractProjectId = (item) => {
  const fromTitle = String(item?.title || '').match(/PROY-[A-Z]+-\d+/i);
  if (fromTitle) return fromTitle[0].toUpperCase();
  const fromMessage = String(item?.message || '').match(/PROY-[A-Z]+-\d+/i);
  if (fromMessage) return fromMessage[0].toUpperCase();
  const legacy = String(item?.title || '').match(/IS-PROY-[A-Z]+-\d+/i);
  if (legacy) return legacy[0].toUpperCase();
  return null;
};

const extractProjectName = (item) => {
  const explicit = item?.projectName || item?.projectNombre || item?.proyectoNombre || item?.project_name || item?.proyecto_nombre;
  if (explicit) return explicit;
  const fromTitle = String(item?.title || '').match(/proyecto:\s*([^"(]+?)(?:\s*\(|$)/i);
  if (fromTitle) return fromTitle[1].trim();
  return null;
};

const sentenceCase = (value) => {
  if (!value) return '';
  const text = String(value).replace(/\s+/g, ' ').trim();
  const looksUpper = text === text.toUpperCase() && /[ÁÉÍÓÚÑA-Z]{4,}/.test(text);
  if (!looksUpper) return text;
  const lower = text.toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
};

const stripHtml = (value) => {
  if (!value) return '';
  let text = String(value);
  text = text.replace(/<style[\s\S]*?<\/style>/gi, ' ');
  text = text.replace(/<script[\s\S]*?<\/script>/gi, ' ');
  text = text.replace(/<br\s*\/?>/gi, '\n');
  text = text.replace(/<\/p>/gi, '\n');
  text = text.replace(/<[^>]+>/g, ' ');
  text = text
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/gi, "'");
  return text;
};

const stripUrls = (value) =>
  String(value || '')
    .replace(/https?:\/\/\S+/gi, '')
    .replace(/\bwww\.\S+/gi, '');

const cleanTitle = (title) => {
  const noLegacy = String(title || '')
    .replace(/\s+en\s+IS-PROY-[A-Z]+-\d+/gi, '')
    .replace(/\s+en\s+PROY-[A-Z]+-\d+/gi, '')
    .trim();
  return sentenceCase(noLegacy);
};

const cleanMessage = (message) => {
  const plain = stripHtml(message);
  const withoutUrls = stripUrls(plain);
  const collapsed = withoutUrls.replace(/\s+/g, ' ').trim();
  if (collapsed.length <= 220) return collapsed;
  return `${collapsed.slice(0, 217).trimEnd()}…`;
};

const getEventMeta = (item) => {
  const code = String(item?.eventCode || item?.codigoEvento || item?.type || '').toUpperCase();
  const severity = String(item?.severity || '').toLowerCase();

  if (code.includes('VIABILIDAD_UPLOADED') || code.includes('DOCUMENT_UPLOADED') || code.includes('EVIDENCE_UPLOADED')) {
    return { label: 'Cargue', tone: 'info', icon: Upload };
  }
  if (code.includes('VIABILIDAD_APPROVED') || code.includes('APROB') || code.includes('VERIF')) {
    return { label: 'Verificado', tone: 'success', icon: Eye };
  }
  if (code.includes('VIABILIDAD_RETURNED') || code.includes('OBSERV') || code.includes('RECHAZ') || code.includes('REJECT')) {
    return { label: 'Observado', tone: 'warning', icon: AlertTriangle };
  }
  if (code.includes('REENV') || code.includes('SUBSAN')) {
    return { label: 'Reenviado', tone: 'info', icon: RotateCcw };
  }
  if (code.includes('DILIG') || code.includes('CREAD') || code.includes('REGISTR') || code.includes('COMPLETE') || code.includes('SUBMIT')) {
    return { label: 'Diligenciado', tone: 'success', icon: ClipboardList };
  }
  if (code.includes('OVERDUE') || code === 'ENTREGABLE_OVERDUE_REMINDER') {
    return { label: 'Vencido', tone: 'error', icon: AlertTriangle };
  }
  if (code.includes('DEADLINE') || code.includes('VENCIMIENTO') || code === 'ENTREGABLE_DEADLINE_WARNING') {
    return { label: 'Por vencer', tone: 'warning', icon: Clock3 };
  }
  if (code.includes('CLOSE') || code.includes('CIERRE')) {
    return { label: code.includes('REJECT') ? 'Rechazado' : 'Aprobación', tone: code.includes('REJECT') ? 'error' : 'success', icon: FileCheck };
  }
  if (code.includes('BENEFIT') || code.includes('IMPACT')) {
    return { label: code.includes('REJECT') ? 'Observado' : 'Revisión', tone: code.includes('REJECT') ? 'warning' : 'info', icon: FileCheck };
  }
  if (code.includes('MAIL') || code.includes('EMAIL')) {
    return { label: 'Correo', tone: 'neutral', icon: Mail };
  }

  if (severity === 'error' || severity === 'critical') return { label: 'Alerta', tone: 'error', icon: AlertTriangle };
  if (severity === 'warning') return { label: 'Aviso', tone: 'warning', icon: Clock3 };
  if (severity === 'success') return { label: 'Completado', tone: 'success', icon: CheckCheck };
  if (severity === 'info') return { label: 'Actualización', tone: 'info', icon: BellRing };

  return { label: 'Notificación', tone: 'neutral', icon: BellRing };
};

const filterGroups = [
  {
    label: 'Recordatorios',
    icon: Clock3,
    codes: ['ENTREGABLE_DEADLINE_WARNING', 'ENTREGABLE_OVERDUE_REMINDER', 'PROJECT_DELAY'],
  },
  {
    label: 'Aprobaciones',
    icon: FileCheck,
    codes: ['DELIVERABLE_APPROVED', 'PROJECT_BENEFIT_IMPACT_REVIEWED', 'CLOSURE_APPROVED', 'VIABILIDAD_APPROVED'],
  },
  {
    label: 'Observaciones',
    icon: AlertTriangle,
    codes: ['DELIVERABLE_REJECTED', 'OBSERVATION_SUBSANATED', 'CLOSURE_REJECTED', 'VIABILIDAD_RETURNED'],
  },
  {
    label: 'Envíos',
    icon: Send,
    codes: [
      'DELIVERABLE_EVIDENCE_UPLOADED',
      'PROJECT_DOCUMENT_UPLOADED',
      'VIABILIDAD_UPLOADED',
      'PROJECT_BENEFIT_IMPACT_SUBMITTED',
      'PROJECT_BENEFIT_IMPACT_RESUBMITTED',
    ],
  },
];

const NOTIFICATION_REFRESH_EVENT = 'proyecta:notificaciones:refresh';

const NotificationBell = () => {
  const { backendLoading } = useAuthContext();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [readFilter, setReadFilter] = useState('unread');

  const buildFilterParams = () => {
    const params = { page: 0, size: 100 };
    if (readFilter === 'unread') params.leido = false;
    else if (readFilter === 'read') params.leido = true;
    if (typeFilter !== 'ALL') {
      const group = filterGroups.find((g) => g.label === typeFilter);
      if (group && group.codes.length === 1) {
        params.eventCode = group.codes[0];
      }
    }
    return params;
  };

  const load = async () => {
    if (backendLoading) return;
    try {
      const params = buildFilterParams();
      const [countResponse, listResponse] = await Promise.all([
        securityService.countUnreadNotifications(),
        securityService.listInAppNotifications(params),
      ]);
      const countPayload = countResponse?.data?.data ?? countResponse?.data ?? countResponse ?? 0;
      const listPayload = listResponse?.data?.data ?? listResponse?.data ?? listResponse ?? {};
      setCount(Number(countPayload || 0));
      setItems(Array.isArray(listPayload?.content) ? listPayload.content : Array.isArray(listPayload) ? listPayload : []);
    } catch {
      setCount(0);
      setItems([]);
    }
  };

  useEffect(() => {
    let active = true;
    const refresh = async () => {
      if (!active) return;
      setLoading(true);
      await load();
      if (active) setLoading(false);
    };
    void refresh();
    const interval = window.setInterval(() => void load(), 30000);
    const handleRefresh = () => {
      void load();
    };
    window.addEventListener(NOTIFICATION_REFRESH_EVENT, handleRefresh);
    return () => {
      active = false;
      window.clearInterval(interval);
      window.removeEventListener(NOTIFICATION_REFRESH_EVENT, handleRefresh);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [backendLoading, typeFilter, readFilter]);

  const filteredItems = useMemo(() => {
    if (typeFilter === 'ALL') return items;
    const group = filterGroups.find((g) => g.label === typeFilter);
    if (!group) return items;
    return items.filter((item) => {
      const code = String(item?.eventCode || '').toUpperCase();
      return group.codes.some((c) => code.includes(c) || code === c);
    });
  }, [items, typeFilter]);

  const displayItems = readFilter === 'unread'
    ? filteredItems.filter((item) => !item.readStatus)
    : filteredItems;

  const handleNotificationClick = async (item) => {
    const url = item.targetUrl;
    if (url) {
      setOpen(false);
      navigate(url);
    }
    try {
      await securityService.markNotificationAsRead(item.id);
      setCount((prev) => Math.max(0, prev - 1));
      setItems((prev) => prev.map((n) => (n.id === item.id ? { ...n, readStatus: true } : n)));
    } catch {
      // silent
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await securityService.markAllNotificationsAsRead();
      await load();
    } catch {
      // keep panel open
    }
  };

  return (
    <div className="notification-bell">
      <button type="button" className="notification-bell__button" onClick={() => setOpen((value) => !value)} aria-label="Abrir notificaciones">
        <BellRing size={18} />
        {count > 0 ? <span className="notification-bell__badge">{count > 99 ? '99+' : count}</span> : null}
      </button>

      {open && (
        <div className="notification-bell__modal" role="dialog" aria-modal="true">
          <div className="notification-bell__modal-header">
            <div className="notification-bell__modal-header-text">
              <strong>Notificaciones</strong>
              <span>{count > 0 ? `${count} sin leer` : 'Todo al día'}</span>
            </div>
            <div className="notification-bell__header-actions">
              {displayItems.some((item) => !item.readStatus) && (
                <button type="button" className="notification-bell__mark-all" onClick={handleMarkAllRead} title="Marcar todas como leídas">
                  <CheckCheck size={14} />
                </button>
              )}
              <button type="button" className="notification-bell__close" onClick={() => setOpen(false)} aria-label="Cerrar">
                <X size={16} />
              </button>
            </div>
          </div>

          <div className="notification-bell__filters">
            <div className="notification-bell__filter-row">
              <span className="notification-bell__filter-label"><Filter size={12} /> Tipo</span>
              <div className="notification-bell__filter-chips">
                <button
                  type="button"
                  className={`notification-bell__chip ${typeFilter === 'ALL' ? 'active' : ''}`}
                  onClick={() => setTypeFilter('ALL')}
                >
                  Todas
                </button>
                {filterGroups.map((group) => {
                  const GroupIcon = group.icon;
                  return (
                    <button
                      key={group.label}
                      type="button"
                      className={`notification-bell__chip ${typeFilter === group.label ? 'active' : ''}`}
                      onClick={() => setTypeFilter(group.label)}
                    >
                      <GroupIcon size={12} />
                      {group.label}
                    </button>
                  );
                })}
              </div>
            </div>
            <div className="notification-bell__filter-row">
              <span className="notification-bell__filter-label"><RotateCcw size={12} /> Estado</span>
              <div className="notification-bell__filter-chips">
                <button
                  type="button"
                  className={`notification-bell__chip ${readFilter === 'unread' ? 'active' : ''}`}
                  onClick={() => setReadFilter('unread')}
                >
                  No leídas
                </button>
                <button
                  type="button"
                  className={`notification-bell__chip ${readFilter === 'all' ? 'active' : ''}`}
                  onClick={() => setReadFilter('all')}
                >
                  Todas
                </button>
                <button
                  type="button"
                  className={`notification-bell__chip ${readFilter === 'read' ? 'active' : ''}`}
                  onClick={() => setReadFilter('read')}
                >
                  Leídas
                </button>
              </div>
            </div>
          </div>

          <div className="notification-bell__modal-body">
            {loading ? (
              <div className="notification-bell__state">
                <LoaderCircle size={16} className="animate-spin" />
                <span>Cargando...</span>
              </div>
            ) : displayItems.length === 0 ? (
              <div className="notification-bell__state">
                <CheckCheck size={16} />
                <span>{readFilter === 'unread' ? 'No hay notificaciones pendientes.' : 'No hay notificaciones para este filtro.'}</span>
              </div>
            ) : (
              displayItems.map((item) => {
                const projectId = extractProjectId(item);
                const projectName = extractProjectName(item);
                const { label, tone, icon: EventIcon } = getEventMeta(item);
                const unread = !item.readStatus;
                return (
                  <button
                    key={item.id}
                    type="button"
                    className={`notification-bell__item ${unread ? 'is-unread' : ''}`}
                    onClick={() => handleNotificationClick(item)}
                  >
                    <div className="notification-bell__item-main">
                      <div className={`notification-bell__item-icon ${tone}`}>
                        <EventIcon size={14} />
                      </div>
                      <div className="notification-bell__item-content">
                        <div className="notification-bell__item-header">
                          <span className="notification-bell__item-title">{cleanTitle(item.title)}</span>
                          <span className="notification-bell__item-date">{compactTime(item.createdAt)}</span>
                        </div>
                        <div className="notification-bell__item-meta">
                          <span className={`notification-bell__item-event ${tone}`}>{label}</span>
                          {(projectName || projectId) && (
                            <span className="notification-bell__item-project">
                              <FolderKanban size={11} />
                              {projectName || projectId}
                              {projectName && projectId ? ` · ${projectId}` : ''}
                            </span>
                          )}
                        </div>
                        <p className="notification-bell__item-message">{cleanMessage(item.message)}</p>
                      </div>
                      {unread && <span className="notification-bell__item-dot" aria-hidden="true" />}
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
