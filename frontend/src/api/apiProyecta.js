import axios from 'axios';
import { auth } from '../utils/auth';

const apiProyecta = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8081/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

apiProyecta.interceptors.request.use(async (config) => {
  try {
    const user = await auth.getUser();
    if (user?.access_token) {
      config.headers.Authorization = `Bearer ${user.access_token}`;
    }
  } catch (error) {
    console.error('Error obteniendo el usuario para el interceptor HTTP', error);
  }

  return config;
}, (error) => Promise.reject(error));

apiProyecta.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.warn('Sesion invalida, redirigiendo al login de Keycloak...');
      auth.signinRedirect().catch((redirectError) => {
        console.error('No se pudo redirigir al login:', redirectError);
      });
    }

    return Promise.reject(error);
  }
);

export default apiProyecta;
