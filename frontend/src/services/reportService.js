import apiClient from '../api/axiosConfig';

const normalizeProjectId = (id) => String(id || '').trim().toUpperCase();

const unwrap = (response) => response?.data?.data ?? response?.data ?? null;

const reportService = {
  getConfigs: async () => {
    const response = await apiClient.get('/reportes/configuracion');
    return unwrap(response);
  },

  getProjectPreview: async (projectId) => {
    const response = await apiClient.get(`/reportes/vista-previa/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  getAllProjectsSummary: async () => {
    const response = await apiClient.get('/reportes/todos-los-proyectos');
    return unwrap(response);
  },

  getDelayedProjects: async () => {
    const response = await apiClient.get('/reportes/proyectos-con-retrasos');
    return unwrap(response);
  },

  getFurag: async (projectId) => {
    const response = await apiClient.get(`/reportes/furag/${normalizeProjectId(projectId)}`);
    return unwrap(response);
  },

  getRiesgosVerificacion: async () => {
    const response = await apiClient.get('/reportes/riesgos');
    return unwrap(response);
  },

  downloadProjectPdf: async (projectId, detailMode = 'resumido') => {
    const response = await apiClient.get(`/reportes/proyecto/${normalizeProjectId(projectId)}/descargar`, {
      params: { detailMode },
      responseType: 'blob',
    });
    return response.data;
  },

  downloadPortafolioPdf: async () => {
    const response = await apiClient.get('/reportes/portafolio/descargar', {
      responseType: 'blob',
    });
    return response.data;
  },

  downloadDelayedProjectsPdf: async () => {
    const response = await apiClient.get('/reportes/proyectos-con-retrasos/descargar', {
      responseType: 'blob',
    });
    return response.data;
  },

  downloadPlanComunicacionesPdf: async () => {
    const response = await apiClient.get('/reportes/plan-comunicaciones/descargar', {
      responseType: 'blob',
    });
    return response.data;
  },

  downloadFuragPdf: async (projectId) => {
    const response = await apiClient.get(`/reportes/furag/${normalizeProjectId(projectId)}/descargar`, {
      responseType: 'blob',
    });
    return response.data;
  },

  downloadRiesgosPdf: async () => {
    const response = await apiClient.get('/reportes/riesgos/descargar', {
      responseType: 'blob',
    });
    return response.data;
  },

  downloadPortafolioExcel: async (params = {}) => {
    const response = await apiClient.get('/reportes/portafolio/excel', {
      params,
      responseType: 'blob',
    });
    return response.data;
  },
};

export default reportService;
