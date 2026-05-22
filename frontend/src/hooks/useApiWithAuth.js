import { useCallback } from 'react';
import { useAuthContext } from '../context/AuthContext';

export function useApiWithAuth() {
  const { accessToken, isAuthenticated } = useAuthContext();

  const callApi = useCallback(async (endpoint, options = {}) => {
    if (!isAuthenticated || !accessToken) {
      throw new Error('Usuario no autenticado');
    }

    const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8081/api/v1';
    const url = `${baseUrl}${endpoint}`;

    const headers = {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
      ...options.headers,
    };

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Error ${response.status}: ${error}`);
    }

    return response.json();
  }, [accessToken, isAuthenticated]);

  return { callApi, accessToken, isAuthenticated };
}
