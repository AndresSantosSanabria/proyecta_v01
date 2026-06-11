import axios from 'axios';
import { auth } from '../utils/auth';
import { appConfig } from '../config/env';

const apiClient = axios.create({
  baseURL: appConfig.apiUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  async (config) => {
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      if (typeof config.headers?.delete === 'function') {
        config.headers.delete('Content-Type');
      } else if (config.headers) {
        delete config.headers['Content-Type'];
        delete config.headers['content-type'];
      }
    }

    if (appConfig.enableAuth) {
      const user = await auth.getUser();
      const token = user?.access_token;
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

export default apiClient;
