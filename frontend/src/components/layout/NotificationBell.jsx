import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { BellRing, CheckCheck, LoaderCircle, X, FolderKanban, CheckCheckIcon } from 'lucide-react';
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

  const text = `${item?.title || ''} ${item?.message || ''}`.toUpperCase();
  if (text.includes('APROB')) return 'Aprobado';
  if (text.includes('REENV')) return 'Reenviado';
  if (text.includes('OBSERV') || text.includes('RECHAZ')) return 'Observado';
  if (text.includes('DILIG') || text.includes('GUARD')) return 'Diligenciado';

  return 'Notificación';
};

const getEventTone = (item) => {
  const label = getEventLabel(item);
  if (label === 'Aprobado' || label === 'Diligenciado') return 'success';
  if (label === 'Observado') return 'warning';
  if (label === 'Reenviado') return 'info';
  return 'neutral';
};

const NOTIFICATION_REFRESH_EVENT = 'proyecta:notificaciones:refresh';

const NotificationBell = () => {
  const { backendLoading } = useAuthContext();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);

  const load = async () => {
    if (backendLoading) return;
    try {
      const [countResponse, listResponse] = await Promise.all([
        securityService.countUnreadNotifications(),
        securityService.listInAppNotifications({ page: 0, size: 100 }),
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
  }, [backendLoading]);

  const unreadItems = useMemo(() => items.filter((item) => !item.readStatus), [items]);

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
              {unreadItems.length > 0 && (
                <button type="button" className="notification-bell__mark-all" onClick={handleMarkAllRead} title="Marcar todas como leídas">
                  <CheckCheck size={14} />
                </button>
              )}
              <button type="button" className="notification-bell__close" onClick={() => setOpen(false)} aria-label="Cerrar">
                <X size={16} />
              </button>
            </div>
          </div>

          <div className="notification-bell__modal-body">
            {loading ? (
              <div className="notification-bell__state">
                <LoaderCircle size={16} className="animate-spin" />
                <span>Cargando...</span>
              </div>
            ) : unreadItems.length === 0 ? (
              <div className="notification-bell__state">
                <CheckCheck size={16} />
                <span>No hay notificaciones pendientes.</span>
              </div>
            ) : (
              unreadItems.map((item) => {
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
