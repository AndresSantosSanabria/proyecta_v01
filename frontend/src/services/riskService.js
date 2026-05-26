import apiClient from '../api/axiosConfig';

const riskService = {
  getRiskList: async (proyectoId) => {
    const { data } = await apiClient.get(`/proyectos/${proyectoId}/riesgos`);
    return data;
  },

  getRiskMatrix: async () => {
    const { data } = await apiClient.get('/proyectos/riesgos/matriz');
    return data;
  },

  createRisk: async (proyectoId, payload) => {
    const { data } = await apiClient.post(`/proyectos/${proyectoId}/riesgos`, payload);
    return data;
  },

  updateRisk: async (proyectoId, riesgoId, payload) => {
    const { data } = await apiClient.put(`/proyectos/${proyectoId}/riesgos/${riesgoId}`, payload);
    return data;
  },

  deleteRisk: async (proyectoId, riesgoId) => {
    const { data } = await apiClient.delete(`/proyectos/${proyectoId}/riesgos/${riesgoId}`);
    return data;
  },
};

export default riskService;
