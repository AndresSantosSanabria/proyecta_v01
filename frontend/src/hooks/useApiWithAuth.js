import { useCallback } from 'react';
import { useAuthContext } from '../context/AuthContext';
import { appConfig } from '../config/env';
import { renewAccessToken, startLoginRedirect } from '../utils/auth';
import { notifyAuth401, notifyAuth403 } from '../utils/feedback';

const buildHeaders = (accessToken, headers = {}) => ({
  'Content-Type': 'application/json',
  Authorization: `Bearer ${accessToken}`,
  ...headers,
});

const parseResponseBody = async (response) => {
  const contentType = response.headers.get('content-type') || '';
  if (response.status === 204) {
    return null;
  }

  if (contentType.includes('application/json')) {
    return response.json();
  }

  return response.text();
};

export function useApiWithAuth() {
  const { accessToken, isAuthenticated } = useAuthContext();

  const callApi = useCallback(async (endpoint, options = {}) => {
    if (!isAuthenticated || !accessToken) {
      throw new Error('Usuario no autenticado');
    }

    const url = `${appConfig.apiUrl}${endpoint}`;
    const requestOptions = {
      ...options,
      headers: buildHeaders(accessToken, options.headers),
    };

    const executeRequest = async (overrideOptions = requestOptions) => fetch(url, overrideOptions);

    const response = await executeRequest();

    if (response.ok) {
      return parseResponseBody(response);
    }

    if (response.status === 403) {
      const backendMessage = await parseResponseBody(response);
      notifyAuth403({
        message: typeof backendMessage === 'string'
          ? backendMessage
          : 'El token es válido, pero no tienes permisos para esta acción.',
      });
      throw new Error(`Error 403: ${typeof backendMessage === 'string' ? backendMessage : JSON.stringify(backendMessage)}`);
    }

    if (response.status === 401 && !options.skipAuthRefresh && !options._retry) {
      try {
        const renewedUser = await renewAccessToken();
        const renewedToken = renewedUser?.access_token;

        if (!renewedToken) {
          throw new Error('No se obtuvo un access token renovado');
        }

        const retryResponse = await executeRequest({
          ...requestOptions,
          _retry: true,
          headers: buildHeaders(renewedToken, options.headers),
        });

        if (retryResponse.ok) {
          return parseResponseBody(retryResponse);
        }

        if (retryResponse.status === 403) {
          const retryBackendMessage = await parseResponseBody(retryResponse);
          notifyAuth403({
            message: typeof retryBackendMessage === 'string'
              ? retryBackendMessage
              : 'El token es válido, pero no tienes permisos para esta acción.',
          });
          throw new Error(`Error 403: ${typeof retryBackendMessage === 'string' ? retryBackendMessage : JSON.stringify(retryBackendMessage)}`);
        }

        if (retryResponse.status === 401) {
          notifyAuth401({
            message: 'Tu sesión expiró y no fue posible renovarla desde este módulo.',
          });
          await startLoginRedirect();
        }

        const retryError = await parseResponseBody(retryResponse);
        throw new Error(`Error ${retryResponse.status}: ${typeof retryError === 'string' ? retryError : JSON.stringify(retryError)}`);
      } catch (refreshError) {
        notifyAuth401({
          message: 'Tu sesión expiró y no fue posible renovarla. Inicia sesión nuevamente.',
        });
        await startLoginRedirect().catch((redirectError) => {
          console.error('No se pudo redirigir al login después de fallar el refresh:', redirectError);
        });
        throw refreshError;
      }
    }

    const error = await parseResponseBody(response);
    throw new Error(`Error ${response.status}: ${typeof error === 'string' ? error : JSON.stringify(error)}`);
  }, [accessToken, isAuthenticated]);

  return { callApi, accessToken, isAuthenticated };
}
