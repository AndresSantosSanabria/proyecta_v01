import { useEffect, useMemo, useState } from 'react';
import {
  BellRing,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Eye,
  FileText,
  LoaderCircle,
  Pencil,
  RefreshCw,
  Save,
  Search,
  ShieldAlert,
  ShieldCheck,
  ToggleLeft,
  ToggleRight,
  X,
} from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import securityService from '../../services/securityService';
import './NotificationTemplatesPanel.css';

const emptyForm = {
  eventCode: '',
  enabled: true,
  htmlEnabled: false,
  severity: 'INFO',
  scope: 'GLOBAL',
  subjectTemplate: '',
  bodyTemplate: '',
  targetRoles: '',
};

const categoryMeta = {
  SECURITY: {
    label: 'Seguridad',
    className: 'cat-security',
    icon: ShieldAlert,
  },
  BUSINESS: {
    label: 'Flujo de Contratos',
    className: 'cat-business',
    icon: FileText,
  },
  SYSTEM: {
    label: 'Sistema',
    className: 'cat-system',
    icon: ShieldCheck,
  },
  DEFAULT: {
    label: 'General',
    className: 'cat-default',
    icon: BellRing,
  },
};

const channelMeta = {
  EMAIL: { label: 'EMAIL', className: 'channel-email', icon: FileText },
};

const severityMeta = {
  INFO: { label: 'Informativa', className: 'severity-info' },
  SUCCESS: { label: 'Exitosa', className: 'severity-success' },
  WARNING: { label: 'Advertencia', className: 'severity-warning' },
  ALERT: { label: 'Crítica', className: 'severity-alert' },
};

const categoryPriority = {
  BUSINESS: 0,
  SECURITY: 1,
  SYSTEM: 2,
  DEFAULT: 3,
};

const fallbackEvents = [
  {
    code: 'BENEFICIO_IMPACTO_DILIGENCIADO',
    name: 'Beneficio e impacto diligenciado',
    category: 'BUSINESS',
    active: true,
    defaultEnabled: true,
    requiresProjectContext: true,
  },
  {
    code: 'BENEFICIO_IMPACTO_OBSERVADO',
    name: 'Beneficio e impacto observado',
    category: 'BUSINESS',
    active: true,
    defaultEnabled: true,
    requiresProjectContext: true,
  },
  {
    code: 'BENEFICIO_IMPACTO_REENVIADO',
    name: 'Beneficio e impacto reenviado',
    category: 'BUSINESS',
    active: true,
    defaultEnabled: true,
    requiresProjectContext: true,
  },
  {
    code: 'BENEFICIO_IMPACTO_APROBADO',
    name: 'Beneficio e impacto aprobado',
    category: 'BUSINESS',
    active: true,
    defaultEnabled: true,
    requiresProjectContext: true,
  },
];

const templateVariables = [
  { key: 'nombre_usuario', label: 'Nombre del usuario', description: 'Usuario o destinatario del evento.' },
  { key: 'enlace_aprobacion', label: 'Enlace de aprobación', description: 'Link seguro para aprobar o revisar.' },
  { key: 'monto', label: 'Monto', description: 'Valor económico asociado al proceso.' },
  { key: 'proyecto_nombre', label: 'Nombre del proyecto', description: 'Proyecto relacionado con la alerta.' },
  { key: 'entregable_nombre', label: 'Nombre del entregable', description: 'Elemento del flujo documental.' },
  { key: 'estado_anterior', label: 'Estado anterior', description: 'Estado previo al cambio de negocio.' },
  { key: 'estado_nuevo', label: 'Estado nuevo', description: 'Estado después del cambio.' },
];

const extractApiDetail = (error) => error?.response?.data?.detail || error?.response?.data?.title || error?.message || '';

const formatDateTime = (value) => {
  if (!value) return 'Sin edición';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Sin edición';
  return new Intl.DateTimeFormat('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(date);
};

const NotificationTemplatesPanel = () => {
  const { user: authUser } = useAuthContext();
  const [events, setEvents] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [preferences, setPreferences] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [preview, setPreview] = useState({ subject: '', body: '' });
  const [selectedCode, setSelectedCode] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [isEditorOpen, setIsEditorOpen] = useState(false);
  const [showInactive, setShowInactive] = useState(false);
  const [categoryFilter, setCategoryFilter] = useState('BUSINESS');
  const [globalNotifCheck, setGlobalNotifCheck] = useState(false);
  const [notifStats, setNotifStats] = useState(null);

  const preferenceUsername = authUser?.profile?.preferred_username
    || authUser?.profile?.username
    || authUser?.profile?.email
    || 'admin';

  const pageSize = 6;

  const loadData = async () => {
    try {
      setLoading(true);
      setError('');
      const [eventsResult, templatesResult, preferencesResult, profileResult, statsResult] = await Promise.allSettled([
        securityService.listNotificationEvents(),
        securityService.listNotificationTemplates(),
        securityService.listNotificationPreferences(),
        securityService.getCurrentProfile(),
        securityService.getNotificationStats(),
      ]);

      const eventData = eventsResult.status === 'fulfilled' ? (eventsResult.value?.data || eventsResult.value || []) : [];
      const templateData = templatesResult.status === 'fulfilled' ? (templatesResult.value?.data || templatesResult.value || []) : [];
      const preferenceData = preferencesResult.status === 'fulfilled' ? (preferencesResult.value?.data || preferencesResult.value || []) : [];

      const mergedEvents = Array.isArray(eventData) ? [...eventData] : [];
      fallbackEvents.forEach((fallbackEvent) => {
        if (!mergedEvents.some((event) => event.code === fallbackEvent.code)) {
          mergedEvents.push(fallbackEvent);
        }
      });
      setEvents(mergedEvents);
      setTemplates(Array.isArray(templateData) ? templateData : []);
      setPreferences(Array.isArray(preferenceData) ? preferenceData : []);

      if (statsResult.status === 'fulfilled') {
        const statsData = statsResult.value?.data?.data || statsResult.value?.data || statsResult.value || null;
        setNotifStats(statsData);
      }

      if (profileResult.status === 'fulfilled') {
        const profile = profileResult.value?.data?.data || profileResult.value?.data || profileResult.value || {};
        setGlobalNotifCheck(Boolean(profile.recibirNotificacionesGlobales));
      }

      const activeEvents = Array.isArray(eventData) ? eventData.filter((event) => event?.active !== false) : [];
      const preferredInitialEvent = activeEvents.find((event) => event.category === 'BUSINESS')
        || activeEvents.find((event) => event.category === 'SECURITY')
        || activeEvents[0]
        || null;
      const firstTemplate = Array.isArray(templateData)
        ? templateData.find((item) => item.eventCode === preferredInitialEvent?.code)
        : null;

      const initialEvent = preferredInitialEvent || (firstTemplate ? { code: firstTemplate.eventCode, name: firstTemplate.eventCode } : null);
      setSelectedCode(initialEvent?.code || '');
      setForm({
        ...emptyForm,
        eventCode: initialEvent?.code || firstTemplate?.eventCode || '',
        enabled: firstTemplate?.enabled ?? initialEvent?.defaultEnabled ?? true,
        htmlEnabled: firstTemplate?.htmlEnabled ?? false,
        severity: firstTemplate?.severity || 'INFO',
        scope: firstTemplate?.scope || 'GLOBAL',
        subjectTemplate: firstTemplate?.subjectTemplate || `Notificación: ${initialEvent?.name || firstTemplate?.eventCode || ''}`,
        bodyTemplate: firstTemplate?.bodyTemplate || 'Hola {{nombre_usuario}}, se generó una notificación para el proyecto {{proyecto_nombre}}.',
      });
    } catch (err) {
      setError(`No fue posible cargar las plantillas. ${extractApiDetail(err)}`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadData();
  }, []);

  const categoryOptions = useMemo(() => {
    const catalogCategories = Array.from(new Set(events.map((event) => event.category).filter(Boolean)));
    return ['ALL', ...catalogCategories];
  }, [events]);

  const visibleEvents = useMemo(() => events.filter((event) => {
    const isActive = event?.active !== false;
    const matchesCategory = categoryFilter === 'ALL' || event?.category === categoryFilter;
    return matchesCategory && (showInactive ? true : isActive);
  }).sort((left, right) => {
    const leftPriority = categoryPriority[left?.category] ?? 99;
    const rightPriority = categoryPriority[right?.category] ?? 99;
    if (leftPriority !== rightPriority) {
      return leftPriority - rightPriority;
    }
    const leftActive = left?.active === false ? 1 : 0;
    const rightActive = right?.active === false ? 1 : 0;
    if (leftActive !== rightActive) {
      return leftActive - rightActive;
    }
    return String(left?.name || left?.code || '').localeCompare(String(right?.name || right?.code || ''));
  }), [categoryFilter, events, showInactive]);

  const rows = useMemo(() => {
    const query = search.trim().toLowerCase();
    const merged = visibleEvents.map((event) => {
      const template = templates.find((item) => item.eventCode === event.code) || null;
      const preference = preferences.find((item) => item.eventCode === event.code) || null;

      return {
        event,
        template,
        preference,
        enabled: template?.enabled ?? event?.defaultEnabled ?? true,
        updatedAt: template?.updatedAt || template?.updated_at || null,
      };
    });

    if (!query) return merged;

    return merged.filter(({ event, template }) => {
      const haystack = [
        event?.name,
        event?.code,
        event?.category,
        template?.subjectTemplate,
        template?.bodyTemplate,
      ].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(query);
    });
  }, [preferences, search, templates, visibleEvents]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setPage(1);
  }, [search]);

  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const currentRows = rows.slice((page - 1) * pageSize, page * pageSize);

  useEffect(() => {
    if (!selectedCode) return;
    const event = events.find((item) => item.code === selectedCode) || null;
    const activeTemplate = templates.find((item) => item.eventCode === selectedCode && item?.enabled !== false) || null;
    const template = activeTemplate || templates.find((item) => item.eventCode === selectedCode) || null;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setForm({
      ...emptyForm,
      eventCode: selectedCode,
      enabled: template?.enabled ?? event?.defaultEnabled ?? true,
      htmlEnabled: template?.htmlEnabled ?? false,
      severity: template?.severity || 'INFO',
      scope: template?.scope || 'GLOBAL',
      subjectTemplate: template?.subjectTemplate || `Notificación: ${event?.name || selectedCode}`,
      bodyTemplate: template?.bodyTemplate || 'Hola {{nombre_usuario}}, se generó una notificación para el proyecto {{proyecto_nombre}}.',
      targetRoles: template?.targetRoles || '',
    });
  }, [events, selectedCode, templates]);

  const stats = useMemo(() => {
    const totalEvents = notifStats?.totalEvents ?? events.length;
    const totalSent = notifStats?.totalSent ?? 0;
    const totalFailed = notifStats?.totalFailed ?? 0;
    const inAppSent = notifStats?.inAppSent ?? 0;
    const inAppFailed = notifStats?.inAppFailed ?? 0;
    const usersWithEmail = notifStats?.usersWithEmail ?? 0;
    const usersWithGlobalNotifs = notifStats?.usersWithGlobalNotifs ?? 0;
    const activeTemplates = notifStats?.activeTemplates ?? templates.filter((t) => t?.enabled !== false).length;
    return { totalEvents, totalSent, totalFailed, inAppSent, inAppFailed, usersWithEmail, usersWithGlobalNotifs, activeTemplates };
  }, [events, notifStats, templates.length]); // eslint-disable-line react-hooks/exhaustive-deps

  const selectedEvent = useMemo(
    () => events.find((event) => event.code === selectedCode) || null,
    [events, selectedCode]
  );

  const selectedCategory = categoryMeta[selectedEvent?.category] || categoryMeta.DEFAULT;
  const selectedSeverity = severityMeta[form.severity] || severityMeta.INFO;

  const handleField = (field) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setForm((current) => ({ ...current, [field]: value }));
  };

  const openEditor = (row) => {
    const code = row?.event?.code || row?.code || selectedCode || events[0]?.code || '';
    const event = events.find((item) => item.code === code) || null;
    if (event?.category && categoryFilter === 'ALL') {
      setCategoryFilter(event.category);
    }
    setSelectedCode(code);
    setPreview({ subject: '', body: '' });
    setIsEditorOpen(true);
  };

  const insertVariable = (key) => {
    setForm((current) => ({
      ...current,
      bodyTemplate: `${current.bodyTemplate}{{${key}}}`,
    }));
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      setError('');
      const payload = {
        ...form,
        eventCode: selectedCode || form.eventCode,
      };
      const saved = await securityService.saveNotificationTemplate(payload);
      const next = saved?.data || saved || payload;
      setTemplates((current) => {
        const filtered = current.filter((item) => item.eventCode !== next.eventCode);
        return [...filtered, next].sort((a, b) => String(a.eventCode).localeCompare(String(b.eventCode)));
      });
      setNotice('Plantilla guardada correctamente.');
      setIsEditorOpen(false);
    } catch (err) {
      setError(`No se pudo guardar la plantilla. ${extractApiDetail(err)}`);
    } finally {
      setSaving(false);
    }
  };

  const handlePreview = async () => {
    try {
      const response = await securityService.previewNotificationTemplate({
        ...form,
        eventCode: selectedCode || form.eventCode,
        variables: {
          nombre_usuario: 'Juan Pérez',
          enlace_aprobacion: 'https://demo.local/aprobar',
          monto: '$ 18.500.000',
          proyecto_nombre: 'Proyecto Demo',
          entregable_nombre: 'Entregable Principal',
          estado_anterior: 'En revisión',
          estado_nuevo: 'Aprobado',
        },
      });

      const payload = response?.data || response || {};
      setPreview({
        subject: payload.subject || payload.asunto || form.subjectTemplate,
        body: payload.body || payload.mensaje || form.bodyTemplate,
      });
    } catch (err) {
      setError(`No fue posible generar el preview. ${extractApiDetail(err)}`);
    }
  };

  const handleTestSend = async () => {
    try {
      setSaving(true);
      setError('');
      const response = await securityService.testSendNotificationTemplate({
        eventCode: selectedCode || form.eventCode,
        subjectTemplate: form.subjectTemplate,
        bodyTemplate: form.bodyTemplate,
        htmlEnabled: form.htmlEnabled,
        variables: {
          nombre_usuario: 'Juan Pérez',
          enlace_aprobacion: 'https://demo.local/aprobar',
          monto: '$ 18.500.000',
          proyecto_nombre: 'Proyecto Demo',
          entregable_nombre: 'Entregable Principal',
          estado_anterior: 'En revisión',
          estado_nuevo: 'Aprobado',
          projectId: 'TEST-001',
          projectName: 'Proyecto Demo',
        },
      });
      const message = response?.message || 'Correo de prueba enviado correctamente';
      setNotice(message);
    } catch (err) {
      const detail = err?.response?.data?.detail || err?.response?.data?.title || err.message;
      setError(`No se pudo enviar el correo de prueba. ${detail}`);
    } finally {
      setSaving(false);
    }
  };

  const toggleTemplateEnabled = async (eventCode, enabled) => {
    try {
      const template = templates.find((item) => item.eventCode === eventCode) || {};
      const payload = {
        ...template,
        eventCode,
        enabled,
      };
      const saved = await securityService.saveNotificationTemplate(payload);
      const next = saved?.data || saved || payload;
      setTemplates((current) => {
        const filtered = current.filter((item) => item.eventCode !== next.eventCode);
        return [...filtered, next];
      });
    } catch (err) {
      setError(`No se pudo actualizar el estado. ${extractApiDetail(err)}`);
    }
  };

  const togglePreference = async (eventCode, enabled) => {
    try {
      const saved = await securityService.saveNotificationPreference({
        username: preferenceUsername,
        eventCode,
        projectId: null,
        enabled,
        emailEnabled: true,
      });
      const next = saved?.data || saved || null;
      if (next) {
        setPreferences((current) => {
          const filtered = current.filter((item) => item.eventCode !== next.eventCode);
          return [...filtered, next];
        });
      }
    } catch (err) {
      setError(`No se pudo actualizar la preferencia. ${extractApiDetail(err)}`);
    }
  };

  return (
    <section className="security-workspace notifications-workspace">
      <article className="panel panel-main notifications-panel">
        <div className="templates-shell">
          <header className="templates-hero">
            <div className="roles-header-copy">
              <p className="security-eyebrow">GESTIÓN DE PLANTILLAS</p>
              <h2>Configuración de notificaciones</h2>
              <p>Administra el canal, el estado y el contenido de cada plantilla con una visual de tabla limpia y edición modal.</p>
            </div>

            <div className="templates-hero__actions">
              <label
                className="global-notif-check"
                title="Activa este check para recibir todas las notificaciones del sistema independientemente del contexto."
              >
                <input
                  type="checkbox"
                  checked={globalNotifCheck}
                  onChange={async (e) => {
                    const next = e.target.checked;
                    setGlobalNotifCheck(next);
                    try {
                      await securityService.updateGlobalNotifications(next);
                      await loadData();
                    } catch {
                      setGlobalNotifCheck(!next);
                      setError('No se pudo actualizar la preferencia de notificaciones globales.');
                    }
                  }}
                />
                <span>Recibir notificaciones globales</span>
              </label>
              <button type="button" className="btn-secondary" onClick={() => void loadData()} disabled={loading}>
                {loading ? <LoaderCircle size={16} className="animate-spin" /> : <RefreshCw size={16} />}
                Actualizar
              </button>
            </div>
          </header>

          {error ? <div className="feedback-banner error">{error}</div> : null}
          {notice ? <div className="feedback-banner success">{notice}</div> : null}

          {globalNotifCheck && (
            <div className="feedback-banner" style={{ background: 'linear-gradient(90deg, #0f766e22, #0f766e11)', border: '1px solid #0f766e55', color: '#0f766e', borderRadius: '8px', padding: '10px 16px', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ShieldCheck size={16} />
              <span><strong>Notificaciones globales activas:</strong> Todas las notificaciones del sistema llegarán a este usuario independientemente del contexto.</span>
            </div>
          )}

          <section className="templates-stats">
            <article className="templates-stat dark">
              <span>Notificaciones enviadas</span>
              <strong>{stats.totalSent + stats.inAppSent}</strong>
              <small>{stats.totalSent} email · {stats.inAppSent} in-app</small>
            </article>
            <article className={`templates-stat ${stats.totalFailed + stats.inAppFailed > 0 ? 'danger' : 'light'}`}>
              <span>Notificaciones fallidas</span>
              <strong>{stats.totalFailed + stats.inAppFailed}</strong>
              <small>{stats.totalFailed} email · {stats.inAppFailed} in-app</small>
            </article>
          </section>

          <div className="table-shell templates-table-shell">
            <div className="table-shell__toolbar">
              <div className="toolbar-filters">
                <div className="inline-search">
                  <Search size={16} />
                  <input
                    type="text"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Buscar plantilla, evento o asunto"
                  />
                </div>
                <select
                  className="catalog-filter"
                  value={categoryFilter}
                  onChange={(event) => setCategoryFilter(event.target.value)}
                >
                  <option value="ALL">Todas las categorías</option>
                  {categoryOptions.filter((option) => option !== 'ALL').map((option) => (
                    <option key={option} value={option}>
                      {categoryMeta[option]?.label || option}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className={`catalog-switch ${showInactive ? 'active' : ''}`}
                  onClick={() => setShowInactive((current) => !current)}
                >
                  {showInactive ? 'Mostrando activos e inactivos' : 'Mostrar solo activos'}
                </button>
              </div>
              <div className="soft-pill">{currentRows.length} visibles</div>
            </div>

            <table className="data-table templates-table">
              <thead>
                <tr>
                  <th>Nombre de plantilla</th>
                  <th>Canal</th>
                  <th>Última edición</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={5} className="table-empty-cell">Cargando plantillas...</td>
                  </tr>
                ) : currentRows.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="table-empty-cell">No hay plantillas para mostrar.</td>
                  </tr>
                ) : currentRows.map(({ event, template, enabled, updatedAt, preference }) => {
                  const category = categoryMeta[event.category] || categoryMeta.DEFAULT;
                  const ChannelIcon = channelMeta.EMAIL.icon;
                  const effectiveEnabled = template?.enabled ?? enabled ?? true;
                  const preferenceEnabled = preference?.enabled ?? true;

                  return (
                    <tr key={event.code}>
                      <td>
                        <div className="template-name-cell">
                          <span className={`template-category-badge ${category.className}`}>
                            <category.icon size={15} />
                          </span>
                          <div>
                            <strong>{event.name}</strong>
                            <span>ID: #{event.code}</span>
                            {event.requiresProjectContext ? <small>Requiere contexto de proyecto</small> : <small>Uso administrativo</small>}
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className={`channel-pill ${channelMeta.EMAIL.className}`}>
                          <ChannelIcon size={12} />
                          {channelMeta.EMAIL.label}
                        </span>
                      </td>
                      <td>{formatDateTime(updatedAt)}</td>
                      <td>
                        <div className="status-stack">
                          <button
                            type="button"
                            className="template-toggle"
                            onClick={() => toggleTemplateEnabled(event.code, !effectiveEnabled)}
                            aria-label="Cambiar estado"
                          >
                            {effectiveEnabled ? <ToggleRight size={30} className="toggle-on" /> : <ToggleLeft size={30} className="toggle-off" />}
                            <span className={`status-chip ${effectiveEnabled ? 'active' : 'inactive'}`}>{effectiveEnabled ? 'Activo' : 'Inactivo'}</span>
                          </button>
                          <button
                            type="button"
                            className="preference-link"
                            onClick={() => togglePreference(event.code, !preferenceEnabled)}
                          >
                            {preferenceEnabled ? 'Notificación interna activa' : 'Notificación interna desactivada'}
                          </button>
                        </div>
                      </td>
                      <td>
                        <button type="button" className="icon-button" onClick={() => openEditor({ event, template })} title="Editar plantilla">
                          <Pencil size={14} />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>

            <div className="pagination-shell">
              <span>
                Mostrando {rows.length === 0 ? 0 : ((page - 1) * pageSize) + 1}
                -
                {Math.min(page * pageSize, rows.length)}
                {' '}
                de {rows.length} plantillas
              </span>
              <div className="pagination-actions">
                <button type="button" className="pagination-btn" onClick={() => setPage((current) => Math.max(1, current - 1))} disabled={page === 1}>
                  <ChevronLeft size={16} />
                </button>
                {Array.from({ length: Math.min(totalPages, 3) }, (_, index) => index + 1).map((item) => (
                  <button
                    key={item}
                    type="button"
                    className={`pagination-number ${page === item ? 'active' : ''}`}
                    onClick={() => setPage(item)}
                  >
                    {item}
                  </button>
                ))}
                <button type="button" className="pagination-btn" onClick={() => setPage((current) => Math.min(totalPages, current + 1))} disabled={page === totalPages}>
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          </div>
        </div>
      </article>

      {isEditorOpen ? (
        <div className="template-modal-backdrop" role="presentation" onMouseDown={() => setIsEditorOpen(false)}>
          <article className="template-modal" role="dialog" aria-modal="true" onMouseDown={(event) => event.stopPropagation()}>
            <header className="template-modal__header">
              <div className="template-modal__title">
                <button type="button" className="template-modal__back" onClick={() => setIsEditorOpen(false)} aria-label="Cerrar">
                  <ChevronLeft size={18} />
                </button>
                <div>
                  <p className="security-eyebrow">MODIFICAR PLANTILLA</p>
                  <h3>{selectedEvent?.name || form.eventCode || 'Plantilla'}</h3>
                  <span style={{ fontSize: '12px', color: '#6b7280' }}>ID: {selectedCode}</span>
                </div>
              </div>
              <div className="template-modal__actions">
                <button type="button" className="btn-secondary" onClick={() => setIsEditorOpen(false)} disabled={saving}>
                  <X size={16} />
                  Cancelar
                </button>
                <button type="button" className="btn-secondary" onClick={() => void handleTestSend()} disabled={saving}>
                  <Eye size={16} />
                  Enviar prueba
                </button>
                <button type="button" className="btn-primary" onClick={() => void handleSave()} disabled={saving}>
                  <Save size={16} />
                  {saving ? 'Guardando...' : 'Guardar cambios'}
                </button>
              </div>
            </header>

            <div className="template-modal__body">
              <aside className="template-modal__sidebar">
                {/* ── Configuración básica ── */}
                <section className="template-block">
                  <h4>Configuración</h4>
                  <label>
                    <span>Nombre del evento</span>
                    <input value={selectedEvent?.name || ''} disabled />
                  </label>
                  <label>
                    <span>Asunto del correo</span>
                    <input
                      value={form.subjectTemplate}
                      onChange={handleField('subjectTemplate')}
                      placeholder="Asunto del correo electrónico"
                    />
                  </label>
                </section>

                {/* ── Estado ── */}
                <section className="template-block">
                  <h4>Estado del canal</h4>
                  <label className="toggle-row">
                    <span>Correo activo</span>
                    <button
                      type="button"
                      className="template-toggle"
                      onClick={() => setForm((current) => ({ ...current, enabled: !current.enabled }))}
                    >
                      {form.enabled ? <ToggleRight size={30} className="toggle-on" /> : <ToggleLeft size={30} className="toggle-off" />}
                      <span className={`status-chip ${form.enabled ? 'active' : 'inactive'}`}>{form.enabled ? 'Activo' : 'Inactivo'}</span>
                    </button>
                  </label>
                  <label className="toggle-row">
                    <span>Formato HTML</span>
                    <button
                      type="button"
                      className="template-toggle"
                      onClick={() => setForm((current) => ({ ...current, htmlEnabled: !current.htmlEnabled }))}
                    >
                      {form.htmlEnabled ? <ToggleRight size={30} className="toggle-on" /> : <ToggleLeft size={30} className="toggle-off" />}
                      <span className={`status-chip ${form.htmlEnabled ? 'active' : 'inactive'}`}>{form.htmlEnabled ? 'HTML' : 'Texto'}</span>
                    </button>
                  </label>
                </section>

                {/* ── Variables de inserción rápida ── */}
                <section className="template-block">
                  <div className="template-block__title">
                    <h4>Insertar variable</h4>
                    <span className="soft-pill">Haz clic para insertar</span>
                  </div>
                  <div className="variables-list">
                    {templateVariables.map((variable) => (
                      <button
                        key={variable.key}
                        type="button"
                        className="variable-chip variable-chip--clickable"
                        title={`${variable.description} — Haz clic para insertar en el cuerpo`}
                        onClick={() => insertVariable(variable.key)}
                      >
                        <span className="variable-chip__token">{`{{${variable.key}}}`}</span>
                        <span className="variable-chip__label">{variable.label}</span>
                      </button>
                    ))}
                  </div>
                  <p style={{ fontSize: '11px', color: '#6b7280', marginTop: '6px' }}>
                    Haz clic en cualquier variable para insertarla automáticamente al final del cuerpo.
                  </p>
                </section>
              </aside>

              <section className="template-modal__editor">
                <div className="template-toolbar">
                  <div className="toolbar-left">
                    <span className={`channel-pill ${selectedCategory.className}`}>{selectedCategory.label}</span>
                    <span className={`channel-pill ${selectedSeverity.className}`}>{selectedSeverity.label}</span>
                    <span className="channel-pill channel-email">
                      {form.htmlEnabled ? 'HTML' : 'Texto plano'}
                    </span>
                  </div>
                </div>

                <label className="editor-field">
                  <span>Cuerpo del correo</span>
                  <textarea
                    value={form.bodyTemplate}
                    onChange={handleField('bodyTemplate')}
                    rows={20}
                    placeholder="Escribe el contenido del correo. Usa las variables del panel izquierdo haciendo clic en ellas."
                  />
                </label>

                <div className="template-modal__preview">
                  <div className="template-modal__preview-head">
                    <h4>Vista previa con datos de prueba</h4>
                    <button type="button" className="btn-secondary" onClick={() => void handlePreview()}>
                      <Eye size={16} />
                      Generar preview
                    </button>
                  </div>
                  <div className="preview-card">
                    <div className="preview-card__header">
                      <div className="preview-card__avatar">
                        <Clock3 size={14} />
                      </div>
                      <div>
                        <strong>{preview.subject || form.subjectTemplate || 'Asunto del preview'}</strong>
                        <span>{selectedEvent?.name || 'Evento seleccionado'}</span>
                      </div>
                    </div>
                    <p>{preview.body || 'Aún no has generado un preview. Presiona "Generar preview" para ver cómo quedará.'}</p>
                  </div>
                </div>
              </section>
            </div>
          </article>
        </div>
      ) : null}
    </section>
  );
};

export default NotificationTemplatesPanel;
