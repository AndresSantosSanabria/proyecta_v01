import { useEffect, useMemo, useState } from 'react';
import { BellRing, CheckCircle2, Clock3, LoaderCircle } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import { usePermission } from '../../hooks/usePermission';
import projectService from '../../services/projectService';
import './NotificationsPage.css';

const normalize = (value) => String(value || '').trim().toUpperCase();

const NotificationsPage = () => {
  const { assignedProjects } = useAuthContext();
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;

    const load = async () => {
      try {
        const response = await projectService.getMyProjects();
        if (!active) return;
        setProjects(Array.isArray(response) ? response : []);
      } catch {
        if (active) setProjects([]);
      } finally {
        if (active) setLoading(false);
      }
    };

    void load();

    return () => {
      active = false;
    };
  }, []);

  const items = useMemo(() => {
    const source = projects.length > 0 ? projects : assignedProjects;
    const mapped = Array.isArray(source) ? source.map((project) => ({
      code: project?.codigo || project?.id || project?.proyectoId || 'SIN-CODIGO',
      name: project?.nombre || project?.nombreProyecto || 'Proyecto',
      state: normalize(project?.estado || project?.status || 'PENDIENTE'),
      progress: Number(project?.avanceTotal || project?.avance || 0),
    })) : [];

    const notifications = [];

    mapped.filter((project) => project.state === 'PENDIENTE').forEach((project) => {
      notifications.push({
        key: `pending-${project.code}`,
        icon: Clock3,
        tone: 'warning',
        title: `${project.name} requiere apertura inicial`,
        detail: `El proyecto ${project.code} sigue en estado pendiente.`,
      });
    });

    mapped.filter((project) => project.progress > 0 && project.progress < 100).slice(0, 4).forEach((project) => {
      notifications.push({
        key: `progress-${project.code}`,
        icon: CheckCircle2,
        tone: 'success',
        title: `${project.name} avanza sin completar`,
        detail: `Lleva ${Math.round(project.progress)}% de avance general.`,
      });
    });

    if (notifications.length === 0) {
      notifications.push({
        key: 'empty',
        icon: BellRing,
        tone: 'neutral',
        title: 'Sin notificaciones nuevas',
        detail: 'No hay alertas activas para tu perfil en este momento.',
      });
    }

    return notifications;
  }, [assignedProjects, projects]);

  const canViewAllNotifications = usePermission('NOTIFICACION:VER_TODAS');

  return (
    <div className="compact-page notifications-page">
      <header className="notifications-page__header">
        <div>
          <h1 className="page-title">Notificaciones</h1>
          <p className="page-subtitle">Alertas operativas compactas para el flujo diario del proyecto.</p>
        </div>
        <span className="soft-pill">{canViewAllNotifications ? 'Vista transversal' : 'Vista asignada'}</span>
      </header>

      {loading ? (
        <div className="notifications-page__loading">
          <LoaderCircle size={18} className="animate-spin" />
          <span>Cargando alertas...</span>
        </div>
      ) : (
        <div className="notifications-page__list">
          {items.map((item) => {
            const Icon = item.icon;
            return (
              <article key={item.key} className={`notification-card ${item.tone}`}>
                <div className="notification-card__icon">
                  <Icon size={16} />
                </div>
                <div className="notification-card__body">
                  <strong>{item.title}</strong>
                  <p>{item.detail}</p>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default NotificationsPage;
