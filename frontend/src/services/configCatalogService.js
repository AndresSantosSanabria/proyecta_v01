import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data?.data ?? response?.data ?? response;

const configCatalogService = {
  getPetiCatalog: async () => {
    const response = await apiClient.get('/configuracion/catalogos/peti');
    return unwrap(response);
  },

  listarParametrica: async (listaClave, soloActivos = true) => {
    const response = await apiClient.get(`/configuracion/listas/${encodeURIComponent(listaClave)}`, {
      params: { soloActivos },
    });
    return unwrap(response);
  },

  listarValoresParametrica: async (listaClave) => {
    const response = await apiClient.get(`/configuracion/listas/${encodeURIComponent(listaClave)}/valores`);
    return unwrap(response);
  },

  guardarParametrica: async (payload) => {
    const response = await apiClient.post('/configuracion/listas', payload);
    return unwrap(response);
  },

  eliminarParametrica: async (id) => {
    const response = await apiClient.delete(`/configuracion/listas/${encodeURIComponent(id)}`);
    return unwrap(response);
  },

  toggleParametrica: async (id) => {
    const response = await apiClient.patch(`/configuracion/listas/${encodeURIComponent(id)}/toggle`);
    return unwrap(response);
  },

  guardarValoresLista: async (listaClave, payload) => {
    const response = await apiClient.put(`/configuracion/listas/${encodeURIComponent(listaClave)}/valores`, payload);
    return unwrap(response);
  },
};

export default configCatalogService;
