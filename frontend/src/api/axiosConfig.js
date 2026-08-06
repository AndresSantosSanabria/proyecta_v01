import axios from 'axios';
import { auth, renewAccessToken, startLoginRedirect } from '../utils/auth';
import { notifyAuth401, notifyAuth403 } from '../utils/feedback';
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

let refreshRequestPromise = null;

const isRefreshable401 = (config) => {
  const url = String(config?.url || '');
  return !config?.skipAuthRefresh
    && !url.includes('/authz/logout')
    && !url.includes('/protocol/openid-connect/');
};

const updateAuthHeader = (config, token) => {
  if (!config) {
    return config;
  }

  config.headers = {
    ...(config.headers || {}),
    Authorization: `Bearer ${token}`,
  };

  return config;
};

const refreshSession = async () => {
  if (!refreshRequestPromise) {
    refreshRequestPromise = (async () => {
      const renewedUser = await renewAccessToken();
      const renewedToken = renewedUser?.access_token || (await auth.getUser())?.access_token;
      if (!renewedToken) {
        throw new Error('No se obtuvo un access token renovado');
      }
      return renewedToken;
    })().finally(() => {
      refreshRequestPromise = null;
    });
  }

  return refreshRequestPromise;
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error.response?.status;
    const originalRequest = error.config;

    if (status === 403) {
      const backendMessage = error.response?.data?.message
        || error.response?.data?.detail
        || 'No tienes permisos para realizar esta acción.';
      notifyAuth403({ message: backendMessage });
      return Promise.reject(error);
    }

    if (status !== 401 || !originalRequest) {
      return Promise.reject(error);
    }

    if (originalRequest._retry || !isRefreshable401(originalRequest)) {
      notifyAuth401();
      startLoginRedirect().catch((redirectError) => {
        console.error('No se pudo redirigir al login tras un 401:', redirectError);
      });
      return Promise.reject(error);
    }

    originalRequest._retry = true;

    try {
      const renewedToken = await refreshSession();
      return apiClient.request(updateAuthHeader(originalRequest, renewedToken));
    } catch (refreshError) {
      console.warn('No fue posible renovar el token tras un 401, redirigiendo al login:', refreshError);
      notifyAuth401({
        message: 'Tu sesión expiró y no fue posible renovarla. Inicia sesión nuevamente.',
      });
      startLoginRedirect().catch((redirectError) => {
        console.error('No se pudo redirigir al login después de fallar el refresh:', redirectError);
      });
      return Promise.reject(error);
    }
  }
);

export default apiClient;
