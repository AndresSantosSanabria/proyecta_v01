import axios from 'axios';
import { auth, startLoginRedirect } from '../utils/auth';
import { appConfig } from '../config/env';

const apiProyecta = axios.create({
  baseURL: appConfig.apiUrl,
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
      console.warn('Sesión inválida, redirigiendo al login de Keycloak...');
      startLoginRedirect().catch((redirectError) => {
        console.error('No se pudo redirigir al login:', redirectError);
      });
    }

    return Promise.reject(error);
  }
);

export default apiProyecta;
