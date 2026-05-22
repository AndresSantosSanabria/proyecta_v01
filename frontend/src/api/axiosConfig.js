import axios from 'axios';
import { auth } from '../utils/auth';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  async (config) => {
    const isAuthEnabled = import.meta.env.VITE_ENABLE_AUTH === 'true';

    if (isAuthEnabled) {
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
