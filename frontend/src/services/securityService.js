import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data ?? response;
const normalizeProjectId = (id) => String(id || '').trim().toUpperCase();

const securityService = {
  getCurrentProfile: async () => {
    const response = await apiClient.get('/usuarios/me');
    return unwrap(response);
  },

  updateGlobalNotifications: async (enabled) => {
    const response = await apiClient.patch('/usuarios/me/notificaciones-globales', {
      recibirNotificacionesGlobales: enabled,
    });
    return unwrap(response);
  },

  getAuthorization: async () => {
    const response = await apiClient.get('/authz/me');
    return unwrap(response);
  },

  listUsers: async ({ search = '', page = 0, size = 20 } = {}) => {
    const response = await apiClient.get('/admin/configuracion/usuarios', {
      params: {
        search: search || undefined,
        page,
        size,
        sort: 'nombre,asc',
      },
    });
    return unwrap(response);
  },

  updateUser: async (payload) => {
    const response = await apiClient.put('/admin/configuracion/usuarios', payload);
    return unwrap(response);
  },

  listRoles: async ({ includeInactive = false } = {}) => {
    const endpoint = includeInactive ? '/admin/configuracion/roles/todos' : '/admin/configuracion/roles';
    const response = await apiClient.get(endpoint);
    return unwrap(response);
  },

  createRole: async (payload) => {
    const response = await apiClient.post('/admin/configuracion/roles', payload);
    return unwrap(response);
  },

  updateRole: async (codigo, payload) => {
    const response = await apiClient.put(`/admin/configuracion/roles/${encodeURIComponent(codigo)}`, payload);
    return unwrap(response);
  },

  deleteRole: async (codigo) => {
    const response = await apiClient.delete(`/admin/configuracion/roles/${encodeURIComponent(codigo)}`);
    return unwrap(response);
  },

  listPermissions: async () => {
    const response = await apiClient.get('/admin/configuracion/permisos');
    return unwrap(response);
  },

  listAssignmentCargos: async () => {
    const response = await apiClient.get('/admin/configuracion/cargos-asignacion');
    return unwrap(response);
  },

  saveRolePermissions: async (matrix) => {
    const response = await apiClient.put('/admin/configuracion/roles-permisos', {
      matriz: matrix,
    });
    return unwrap(response);
  },

  assignUserToProject: async (payload) => {
    const response = await apiClient.post('/admin/configuracion/usuario-proyecto', payload);
    return unwrap(response);
  },

  listAssignments: async (username) => {
    const response = await apiClient.get('/admin/configuracion/usuario-proyecto', {
      params: { username },
    });
    return unwrap(response);
  },

  listNotificationEvents: async () => {
    const response = await apiClient.get('/admin/notificaciones/eventos');
    return unwrap(response);
  },

  listNotificationTemplates: async ({ category, severity, enabled, search } = {}) => {
    const params = {};
    if (category && category !== 'ALL') params.category = category;
    if (severity && severity !== 'ALL') params.severity = severity;
    if (enabled !== undefined && enabled !== null && enabled !== 'ALL') params.enabled = enabled;
    if (search && search.trim()) params.search = search.trim();
    const response = await apiClient.get('/admin/notificaciones/plantillas', { params });
    return unwrap(response);
  },

  saveNotificationTemplate: async (payload) => {
    const response = await apiClient.put('/admin/notificaciones/plantillas', payload);
    return unwrap(response);
  },

  listNotificationPreferences: async () => {
    const response = await apiClient.get('/admin/notificaciones/preferencias');
    return unwrap(response);
  },

  saveNotificationPreference: async (payload) => {
    const response = await apiClient.put('/admin/notificaciones/preferencias', payload);
    return unwrap(response);
  },

  previewNotificationTemplate: async (payload) => {
    const response = await apiClient.post('/admin/notificaciones/plantillas/preview', payload);
    return unwrap(response);
  },

  testSendNotificationTemplate: async (payload) => {
    const response = await apiClient.post('/admin/notificaciones/plantillas/test-send', payload);
    return unwrap(response);
  },

  getMailDiagnostic: async () => {
    const response = await apiClient.get('/admin/notificaciones/diagnostico');
    return unwrap(response);
  },

  getNotificationStats: async () => {
    const response = await apiClient.get('/admin/notificaciones/estadisticas');
    return unwrap(response);
  },

  getFailedNotifications: async ({ page = 0, size = 20, channel, eventCode, recipient, from, to } = {}) => {
    const params = { page, size };
    if (channel && channel !== 'ALL') params.channel = channel;
    if (eventCode && eventCode !== 'ALL') params.eventCode = eventCode;
    if (recipient && recipient.trim()) params.recipient = recipient.trim();
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await apiClient.get('/admin/notificaciones/fallidas', { params });
    return unwrap(response);
  },

  getFailedDispatchLogs: async ({ page = 0, size = 20, recipient, from, to } = {}) => {
    const params = { page, size };
    if (recipient && recipient.trim()) params.recipient = recipient.trim();
    if (from) params.from = from;
    if (to) params.to = to;
    const response = await apiClient.get('/admin/notificaciones/fallidas/detalle-dispatch', { params });
    return unwrap(response);
  },

  listInAppNotifications: async ({ page = 0, size = 10, leido, eventCode } = {}) => {
    const params = { page, size };
    if (leido !== undefined && leido !== null) params.leido = leido;
    if (eventCode && eventCode !== 'ALL') params.eventCode = eventCode;
    const response = await apiClient.get('/notificaciones', { params });
    return unwrap(response);
  },

  countUnreadNotifications: async () => {
    const response = await apiClient.get('/notificaciones/no-leidas');
    return unwrap(response);
  },

  markNotificationAsRead: async (id) => {
    const response = await apiClient.patch(`/notificaciones/${encodeURIComponent(id)}/leer`);
    return unwrap(response);
  },

  markAllNotificationsAsRead: async () => {
    const response = await apiClient.patch('/notificaciones/marcar-todas-leidas');
    return unwrap(response);
  },

  notifyProjectBenefitImpactEvent: async (projectId, payload = {}) => {
    const response = await apiClient.post(
      `/proyectos/${normalizeProjectId(projectId)}/beneficio-impacto/notificar`,
      payload
    );
    return unwrap(response);
  },

  getActiveClosureTemplate: async () => {
    const response = await apiClient.get('/admin/closure-templates/active');
    return unwrap(response);
  },

  saveClosureTemplate: async (payload) => {
    const response = await apiClient.post('/admin/closure-templates', payload);
    return unwrap(response);
  },

  getClosureRecord: async (projectId) => {
    const response = await apiClient.get(`/admin/closure-templates/closure-record/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  saveClosureRecord: async (projectId, formData) => {
    const response = await apiClient.post(`/admin/closure-templates/closure-record/${normalizeProjectId(projectId)}`, { formData });
    return unwrap(response);
  },

  listClosureQuestions: async () => {
    const response = await apiClient.get('/admin/closure-questions');
    return unwrap(response);
  },

  listActiveClosureQuestions: async () => {
    const response = await apiClient.get('/admin/closure-questions/active');
    return unwrap(response);
  },

  createClosureQuestion: async (payload) => {
    const response = await apiClient.post('/admin/closure-questions', payload);
    return unwrap(response);
  },

  updateClosureQuestion: async (id, payload) => {
    const response = await apiClient.patch(`/admin/closure-questions/${id}`, payload);
    return unwrap(response);
  },

  toggleClosureQuestion: async (id) => {
    const response = await apiClient.patch(`/admin/closure-questions/${id}/toggle`);
    return unwrap(response);
  },

  deleteClosureQuestion: async (id) => {
    const response = await apiClient.delete(`/admin/closure-questions/${id}`);
    return unwrap(response);
  },

  getClosureAnswers: async (projectId) => {
    const response = await apiClient.get(`/admin/closure-questions/answers/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  saveClosureAnswers: async (projectId, answers) => {
    const response = await apiClient.post(`/admin/closure-questions/answers/${normalizeProjectId(projectId)}`, answers);
    return unwrap(response);
  },

  getResolvedClosureTemplate: async (projectId) => {
    const response = await apiClient.get(`/admin/closure-questions/resolved-template/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  getClosureDraftTemplate: async (projectId) => {
    const response = await apiClient.get(`/admin/closure-questions/draft/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  getMissingClosureQuestions: async (projectId) => {
    const response = await apiClient.get(`/admin/closure-questions/missing/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  testStoragePath: async (path) => {
    const response = await apiClient.get('/admin/configuracion/storage-path/test', {
      params: { path },
    });
    return response?.data?.data ?? unwrap(response);
  },

  getUserPermissionMatrix: async (usuarioId) => {
    const response = await apiClient.get(`/admin/configuracion/permisos-usuario/${encodeURIComponent(usuarioId)}`);
    return unwrap(response);
  },

  saveUserPermissionMatrix: async (payload) => {
    const response = await apiClient.put('/admin/configuracion/permisos-usuario', payload);
    return unwrap(response);
  },
};

export default securityService;
