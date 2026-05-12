import axios from 'axios';

/**
 * Configuración de instancia de Axios para la comunicación con la API.
 * Se utiliza la URL base definida en las variables de entorno de Vite.
 */
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor para adjuntar el token de autenticación en cada petición (si está habilitado)
apiClient.interceptors.request.use(
  (config) => {
    // Switch para habilitar/deshabilitar autenticación desde el .env
    const isAuthEnabled = import.meta.env.VITE_ENABLE_AUTH === 'true';
    
    if (isAuthEnabled) {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;
