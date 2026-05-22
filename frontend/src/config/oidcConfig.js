/**
 * Configuración OIDC para Keycloak
 * Authority: Servidor de Keycloak
 * Client ID: proyecta-web (tipo público - sin client secret)
 * Flow: Authorization Code con PKCE (S256)
 */

const oidcConfig = {
  authority: 'http://172.20.6.59:8080/realms/gob-cundinamarca-devqa',
  client_id: 'proyecta-web',
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: `${window.location.origin}/`,
  response_type: 'code',
  scope: 'openid profile email',
  loadUserInfo: true,
  automaticSilentRenew: true,
  includeIdTokenInSilentRenew: true,
  monitorSession: false,
};

export default oidcConfig;
