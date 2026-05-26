const keycloakBaseUrl = import.meta.env.VITE_KEYCLOAK_BASE_URL?.replace(/\/+$/, '');
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM;
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;

/**
 * Configuración OIDC para Keycloak.
 * Esta copia usa las mismas variables que `src/utils/auth.js` para evitar drift.
 */
const oidcConfig = {
  authority: `${keycloakBaseUrl}/realms/${keycloakRealm}`,
  client_id: clientId,
  redirect_uri: import.meta.env.VITE_KEYCLOAK_REDIRECT_URI,
  post_logout_redirect_uri: import.meta.env.VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI,
  response_type: 'code',
  scope: 'openid profile email',
  loadUserInfo: true,
  automaticSilentRenew: true,
  includeIdTokenInSilentRenew: true,
  monitorSession: false,
};

export default oidcConfig;
