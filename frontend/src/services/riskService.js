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

  downloadRiskMatrixExcel: async (proyectoId) => {
    const response = await apiClient.get(`/proyectos/${proyectoId}/riesgos/descargar-excel`, {
      responseType: 'blob',
    });
    return response.data;
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

  getRiskSolutions: async (proyectoId, riesgoId) => {
    const { data } = await apiClient.get(`/proyectos/${proyectoId}/riesgos/${riesgoId}/soluciones`);
    return data;
  },

  uploadRiskSolutions: async (proyectoId, riesgoId, files, onUploadProgress) => {
    const formData = new FormData();
    Array.from(files || []).forEach((file) => {
      formData.append('archivos', file);
    });

    const { data } = await apiClient.post(
      `/proyectos/${proyectoId}/riesgos/${riesgoId}/soluciones`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onUploadProgress && progressEvent.total) {
            const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onUploadProgress(percent);
          }
        },
      }
    );
    return data;
  },

  downloadRiskSolution: async (proyectoId, riesgoId, solucionId) => {
    const response = await apiClient.get(
      `/proyectos/${proyectoId}/riesgos/${riesgoId}/soluciones/${solucionId}/descargar`,
      { responseType: 'blob' }
    );
    return response.data;
  },
};

export default riskService;
