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
};

export default securityService;
