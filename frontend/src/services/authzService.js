import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data ?? response;

const authzService = {
  getMe: async () => {
    const response = await apiClient.get('/authz/me');
    return unwrap(response);
  },
};

export default authzService;
