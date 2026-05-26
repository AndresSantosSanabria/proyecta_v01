import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

const keycloakBaseUrl = import.meta.env.VITE_KEYCLOAK_BASE_URL?.replace(/\/+$/, '');
const keycloakRealm = import.meta.env.VITE_KEYCLOAK_REALM;
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;
const authority = `${keycloakBaseUrl}/realms/${keycloakRealm}`;
const redirectUri = import.meta.env.VITE_KEYCLOAK_REDIRECT_URI;
const postLogoutRedirectUri = import.meta.env.VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI;

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
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  metadata,
});

let loginRedirectPromise = null;

export async function clearOidcStaleState() {
  try {
    await auth.clearStaleState();
  } catch (error) {
    console.warn('No fue posible limpiar el estado OIDC previo al login:', error);
  }
}

export async function startLoginRedirect() {
  if (loginRedirectPromise) {
    return loginRedirectPromise;
  }

  loginRedirectPromise = (async () => {
    await clearOidcStaleState();
    return auth.signinRedirect();
  })();

  try {
    return await loginRedirectPromise;
  } finally {
    loginRedirectPromise = null;
  }
}

export async function startLogoutRedirect() {
  try {
    await auth.removeUser();
    await auth.clearStaleState();
  } catch (error) {
    console.warn('No se pudo limpiar el estado OIDC antes del logout:', error);
  }

  return auth.signoutRedirect();
}

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
