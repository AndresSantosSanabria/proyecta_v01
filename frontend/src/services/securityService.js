import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data ?? response;

const securityService = {
  getCurrentProfile: async () => {
    const response = await apiClient.get('/usuarios/me');
    return unwrap(response);
  },

  getActiveRoles: async () => {
    const response = await apiClient.get('/admin/roles/activos');
    return unwrap(response);
  },

  getUsers: async ({ search = '', page = 0, size = 20, sort = 'nombre,asc' } = {}) => {
    const response = await apiClient.get('/admin/usuarios', {
      params: {
        busqueda: search || undefined,
        page,
        size,
        sort,
      },
    });
    return unwrap(response);
  },

  createUser: async (payload) => {
    const response = await apiClient.post('/admin/usuarios', payload);
    return unwrap(response);
  },

  updateUser: async (id, payload) => {
    const response = await apiClient.put(`/admin/usuarios/${id}`, payload);
    return unwrap(response);
  },

  toggleUserState: async (id, activo) => {
    const response = await apiClient.patch(`/admin/usuarios/${id}/estado`, null, {
      params: { activo },
    });
    return unwrap(response);
  },
};

export default securityService;
