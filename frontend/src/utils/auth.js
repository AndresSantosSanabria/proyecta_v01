import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

const keycloakBaseUrl = import.meta.env.VITE_KEYCLOAK_BASE_URL || 'http://172.20.6.59:8080';
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM || 'gob-cundinamarca-devqa';
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'proyecta-web';
const authority = `${keycloakBaseUrl}/realms/${keycloakRealm}`;
const redirectUri = import.meta.env.VITE_KEYCLOAK_REDIRECT_URI || `${window.location.origin}/callback`;
const postLogoutRedirectUri =
  import.meta.env.VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI || `${window.location.origin}/logged-out`;

const metadata = {
  issuer: authority,
  authorization_endpoint: `${authority}/protocol/openid-connect/auth`,
  token_endpoint: `${authority}/protocol/openid-connect/token`,
  end_session_endpoint: `${authority}/protocol/openid-connect/logout`,
  jwks_uri: `${authority}/protocol/openid-connect/certs`,
  userinfo_endpoint: `${authority}/protocol/openid-connect/userinfo`,
};

export const auth = new UserManager({
  authority,
  client_id: clientId,
  redirect_uri: redirectUri,
  post_logout_redirect_uri: postLogoutRedirectUri,
  response_type: 'code',
  scope: 'openid profile email',
  automaticSilentRenew: true,
  monitorSession: false,
  loadUserInfo: false,
  filterProtocolClaims: true,
  userStore: new WebStorageStateStore({ store: window.localStorage }),
  metadata,
});

export function decodeJwtPayload(token) {
  if (!token || typeof token !== 'string') {
    return {};
  }

  try {
    const payload = token.split('.')[1];
    if (!payload) {
      return {};
    }

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const json = window.atob(padded);
    return JSON.parse(json);
  } catch (error) {
    console.error('Error decodificando JWT de Keycloak:', error);
    return {};
  }
}

export function extractRolesFromToken(token) {
  const payload = decodeJwtPayload(token);
  const roles = new Set();

  const realmRoles = payload?.realm_access?.roles;
  if (Array.isArray(realmRoles)) {
    realmRoles.forEach((role) => roles.add(role));
  }

  const resourceRoles = payload?.resource_access?.[clientId]?.roles;
  if (Array.isArray(resourceRoles)) {
    resourceRoles.forEach((role) => roles.add(role));
  }

  return Array.from(roles);
}
