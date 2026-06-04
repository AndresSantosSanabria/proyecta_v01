import { appConfig } from './env';

/**
 * Configuracion OIDC para Keycloak.
 * Esta copia usa las mismas variables que `src/utils/auth.js` para evitar drift.
 */
const oidcConfig = {
  authority: appConfig.keycloak.authority,
  client_id: appConfig.keycloak.clientId,
  redirect_uri: appConfig.keycloak.redirectUri,
  post_logout_redirect_uri: appConfig.keycloak.postLogoutRedirectUri,
  response_type: appConfig.keycloak.responseType,
  scope: appConfig.keycloak.scope,
  loadUserInfo: true,
  automaticSilentRenew: true,
  includeIdTokenInSilentRenew: true,
  monitorSession: false,
};

export default oidcConfig;
