import { useEffect, useMemo, useState } from 'react';
import { BellRing, CheckCheck, LoaderCircle, X, FolderKanban } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import securityService from '../../services/securityService';
import './NotificationBell.css';

const formatTime = (value) => {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

const extractProjectId = (title) => {
  const match = title?.match(/IS-PROY-[A-Z]+-\d+/);
  return match ? match[0] : null;
};

const cleanTitle = (title) => {
  if (!title) return '';
  return title.replace(/ en IS-PROY-[A-Z]+-\d+| en IS-PROY-[A-Z]+-\d+/g, '').trim();
};

const NotificationBell = () => {
  const { backendLoading } = useAuthContext();
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState([]);

  const load = async () => {
    if (backendLoading) return;
    try {
      const [countResponse, listResponse] = await Promise.all([
        securityService.countUnreadNotifications(),
        securityService.listInAppNotifications({ page: 0, size: 8 }),
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
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [backendLoading]);

  const unreadItems = useMemo(() => items.filter((item) => !item.readStatus), [items]);

  const markRead = async (item) => {
    try {
      await securityService.markNotificationAsRead(item.id);
      await load();
    } catch {
      setOpen(true);
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
            <button type="button" className="notification-bell__close" onClick={() => setOpen(false)} aria-label="Cerrar">
              <X size={16} />
            </button>
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
                return (
                  <button key={item.id} type="button" className="notification-bell__item" onClick={() => markRead(item)}>
                    <div className="notification-bell__item-header">
                      <span className="notification-bell__item-title">{cleanTitle(item.title)}</span>
                      <span className="notification-bell__item-date">{formatTime(item.createdAt)}</span>
                    </div>
                    {projectId && (
                      <div className="notification-bell__item-project">
                        <FolderKanban size={11} />
                        {projectId}
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
