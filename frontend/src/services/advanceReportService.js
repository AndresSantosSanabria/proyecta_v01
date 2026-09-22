import apiClient from '../api/axiosConfig';

const normalizeProjectId = (id) => String(id || '').trim().toUpperCase();

const advanceReportService = {
  getStatus: async (projectId) => {
    const { data } = await apiClient.get(`/advance-report/status/${normalizeProjectId(projectId)}`, {
      suppressAuthToast: true,
    });
    return data?.data ?? data;
  },

  downloadReport: async (projectId) => {
    const response = await apiClient.get(`/advance-report/download/${normalizeProjectId(projectId)}`, {
      responseType: 'blob',
    });
    return response.data;
  },

  getPendingProjects: async () => {
    const { data } = await apiClient.get('/advance-report/pending');
    return data?.data ?? data;
  },

  getSettings: async () => {
    const { data } = await apiClient.get('/advance-report/settings');
    return data?.data ?? data;
  },

  updateSettings: async (settings) => {
    const { data } = await apiClient.put('/advance-report/settings', settings);
    return data?.data ?? data;
  },

  uploadReport: async (projectId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    const { data } = await apiClient.post(`/advance-report/upload/${normalizeProjectId(projectId)}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return data?.data ?? data;
  },

  verifyReport: async (projectId) => {
    const { data } = await apiClient.put(`/advance-report/verify/${normalizeProjectId(projectId)}`);
    return data?.data ?? data;
  },

  returnReport: async (projectId, observaciones) => {
    const { data } = await apiClient.put(`/advance-report/return/${normalizeProjectId(projectId)}`, { observaciones });
    return data?.data ?? data;
  },
};

export default advanceReportService;
