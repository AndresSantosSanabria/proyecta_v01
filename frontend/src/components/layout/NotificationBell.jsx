import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BellRing, CheckCheck, Clock3, LoaderCircle, X, FolderKanban, Filter, AlertTriangle, FileCheck, Send, RotateCcw } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import securityService from '../../services/securityService';
import { formatDateTime } from '../../utils/locale';
import './NotificationBell.css';

const formatTime = (value) => {
  if (!value) return '';
  return formatDateTime(value, {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const extractProjectId = (title) => {
  const match = title?.match(/IS-PROY-[A-Z]+-\d+/);
  return match ? match[0] : null;
};

const extractProjectName = (item) => (
  item?.projectName
  || item?.projectNombre
  || item?.proyectoNombre
  || item?.project_name
  || item?.proyecto_nombre
  || null
);

const cleanTitle = (title) => {
  if (!title) return '';
  return title.replace(/ en IS-PROY-[A-Z]+-\d+| en IS-PROY-[A-Z]+-\d+/g, '').trim();
};

const getEventLabel = (item) => {
  const code = String(item?.eventCode || item?.codigoEvento || item?.type || '').toUpperCase();
  if (code.includes('APROB')) return 'Aprobado';
  if (code.includes('REENV') || code.includes('REENVI')) return 'Reenviado';
  if (code.includes('OBSERV') || code.includes('RECHAZ')) return 'Observado';
  if (code.includes('DILIG') || code.includes('CREAD') || code.includes('REGISTR')) return 'Diligenciado';
  if (code === 'ENTREGABLE_OVERDUE_REMINDER' || code.includes('OVERDUE')) return 'Vencido';
  if (code === 'ENTREGABLE_DEADLINE_WARNING' || code.includes('DEADLINE') || code.includes('VENCIMIENTO')) return 'Por vencer';

  const text = `${item?.title || ''} ${item?.message || ''}`.toUpperCase();
  if (text.includes('APROB')) return 'Aprobado';
  if (text.includes('REENV')) return 'Reenviado';
  if (text.includes('OBSERV') || text.includes('RECHAZ')) return 'Observado';
  if (text.includes('DILIG') || text.includes('GUARD')) return 'Diligenciado';
  if (text.includes('VENCID') || text.includes('ATRAS')) return 'Vencido';
  if (text.includes('POR VENCER') || text.includes('VENCE')) return 'Por vencer';

  return 'Notificación';
};

const getEventTone = (item) => {
  const label = getEventLabel(item);
  if (label === 'Aprobado' || label === 'Diligenciado') return 'success';
  if (label === 'Observado') return 'warning';
  if (label === 'Reenviado') return 'info';
  if (label === 'Vencido') return 'error';
  if (label === 'Por vencer') return 'warning';
  return 'neutral';
};

const filterGroups = [
  {
    label: 'Recordatorios',
    icon: Clock3,
    codes: ['ENTREGABLE_DEADLINE_WARNING', 'ENTREGABLE_OVERDUE_REMINDER'],
  },
  {
    label: 'Aprobaciones',
    icon: FileCheck,
    codes: ['DELIVERABLE_APPROVED', 'PROJECT_BENEFIT_IMPACT_REVIEWED', 'CLOSURE_APPROVED'],
  },
  {
    label: 'Observaciones',
    icon: AlertTriangle,
    codes: ['DELIVERABLE_REJECTED', 'OBSERVATION_SUBSANATED', 'CLOSURE_REJECTED'],
  },
  {
    label: 'Envíos',
    icon: Send,
    codes: ['DELIVERABLE_EVIDENCE_UPLOADED', 'PROJECT_DOCUMENT_UPLOADED', 'PROJECT_BENEFIT_IMPACT_SUBMITTED', 'PROJECT_BENEFIT_IMPACT_RESUBMITTED'],
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
    try {
      await securityService.markNotificationAsRead(item.id);
      await load();
      if (item.targetUrl) navigate(item.targetUrl);
    } catch {
      // keep panel open
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
        {count > 0 ? <span className="notification-bell__badge">{count}</span> : null}
      </button>

      {open && (
        <div className="notification-bell__modal" role="dialog" aria-modal="true">
          <div className="notification-bell__modal-header">
            <div className="notification-bell__modal-header-text">
              <strong>Notificaciones</strong>
              <span>{count} sin leer</span>
            </div>
            <div className="notification-bell__header-actions">
              {displayItems.length > 0 && (
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
                const projectId = extractProjectId(item.title);
                const projectName = extractProjectName(item);
                const eventLabel = getEventLabel(item);
                const eventTone = getEventTone(item);
                return (
                  <button key={item.id} type="button" className="notification-bell__item" onClick={() => handleNotificationClick(item)}>
                    <div className="notification-bell__item-header">
                      <span className="notification-bell__item-title">{cleanTitle(item.title)}</span>
                      <span className="notification-bell__item-date">{formatTime(item.createdAt)}</span>
                    </div>
                    <div className={`notification-bell__item-event ${eventTone}`}>
                      {eventLabel}
                    </div>
                    {(projectName || projectId) && (
                      <div className="notification-bell__item-project">
                        <FolderKanban size={11} />
                        {projectName || projectId}
                        {projectName && projectId ? ` · ${projectId}` : ''}
                      </div>
                    )}
                    <p className="notification-bell__item-message">{item.message}</p>
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
