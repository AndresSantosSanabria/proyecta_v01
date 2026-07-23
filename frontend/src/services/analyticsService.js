import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data?.data ?? response?.data ?? null;

const analyticsService = {
  getPortfolio: async () => {
    const response = await apiClient.get('/analytics/portafolio');
    return unwrap(response);
  },

  downloadPortfolioPdf: async () => {
    const response = await apiClient.get('/analytics/portafolio/pdf', {
      responseType: 'blob',
    });
    return response.data;
  },
};

export default analyticsService;
