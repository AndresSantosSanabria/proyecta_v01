const requiredEnv = (key) => {
  const value = import.meta.env[key];
  if (value === undefined || value === null || value.toString().trim() === '') {
    throw new Error(`Falta configurar la variable de entorno requerida: ${key}`);
  }
  return value.toString().trim();
};

const optionalEnv = (key, fallback = '') => {
  const value = import.meta.env[key];
  return value === undefined || value === null ? fallback : value.toString().trim();
};

const keycloakBaseUrl = requiredEnv('VITE_KEYCLOAK_BASE_URL').replace(/\/+$/, '');
const keycloakRealm = requiredEnv('VITE_KEYCLOAK_REALM');

export const appConfig = {
  apiUrl: requiredEnv('VITE_API_URL').replace(/\/+$/, ''),
  enableAuth: requiredEnv('VITE_ENABLE_AUTH') === 'true',
  keycloak: {
    baseUrl: keycloakBaseUrl,
    realm: keycloakRealm,
    authority: `${keycloakBaseUrl}/realms/${keycloakRealm}`,
    clientId: requiredEnv('VITE_KEYCLOAK_CLIENT_ID'),
    redirectUri: requiredEnv('VITE_KEYCLOAK_REDIRECT_URI'),
    postLogoutRedirectUri: requiredEnv('VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI'),
    responseType: optionalEnv('VITE_KEYCLOAK_RESPONSE_TYPE', 'code'),
    scope: requiredEnv('VITE_KEYCLOAK_SCOPE'),
  },
};
