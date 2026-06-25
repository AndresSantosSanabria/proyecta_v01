import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data ?? response;

const securityService = {
  getCurrentProfile: async () => {
    const response = await apiClient.get('/usuarios/me');
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

  listSystemParameters: async () => {
    const response = await apiClient.get('/admin/configuracion/parametros');
    return unwrap(response);
  },

  saveSystemParameter: async (payload) => {
    const response = await apiClient.put('/admin/configuracion/parametros', payload);
    return unwrap(response);
  },

  deleteSystemParameter: async (key) => {
    const response = await apiClient.delete(`/admin/configuracion/parametros/${encodeURIComponent(key)}`);
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

  listNotificationTemplates: async () => {
    const response = await apiClient.get('/admin/notificaciones/plantillas');
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

  listInAppNotifications: async ({ page = 0, size = 10 } = {}) => {
    const response = await apiClient.get('/notificaciones', { params: { page, size } });
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
};

export default securityService;
