import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data?.data ?? response?.data ?? response;

const configCatalogService = {
  getPetiCatalog: async () => {
    const response = await apiClient.get('/configuracion/catalogos/peti');
    return unwrap(response);
  },
};

export default configCatalogService;
