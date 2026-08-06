import { UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { appConfig } from '../config/env';
import { prepareLogoutSession } from '../services/sessionService';
import { notifyLogout } from './feedback';

const clientId = appConfig.keycloak.clientId;
const authority = appConfig.keycloak.authority;

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
  redirect_uri: appConfig.keycloak.redirectUri,
  post_logout_redirect_uri: appConfig.keycloak.postLogoutRedirectUri,
  response_type: appConfig.keycloak.responseType,
  scope: appConfig.keycloak.scope,
  automaticSilentRenew: true,
  monitorSession: false,
  loadUserInfo: false,
  filterProtocolClaims: true,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
  metadata,
});

let loginRedirectPromise = null;
let silentRenewPromise = null;
let logoutRedirectPromise = null;

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

export async function renewAccessToken() {
  if (silentRenewPromise) {
    return silentRenewPromise;
  }

  silentRenewPromise = (async () => {
    await auth.signinSilent();
    return auth.getUser();
  })();

  try {
    return await silentRenewPromise;
  } finally {
    silentRenewPromise = null;
  }
}

async function clearLocalOidcState() {
  try {
    await auth.removeUser();
    await auth.clearStaleState();
  } catch (error) {
    console.warn('No se pudo limpiar el estado OIDC antes del logout:', error);
  }
}

export async function startLogoutRedirect() {
  if (logoutRedirectPromise) {
    return logoutRedirectPromise;
  }

  logoutRedirectPromise = (async () => {
    let currentUser = null;

    try {
      currentUser = await auth.getUser();
    } catch (error) {
      console.warn('No fue posible leer el usuario actual antes del logout:', error);
    }

    const accessToken = currentUser?.access_token ?? null;
    const idToken = currentUser?.id_token ?? null;
    const refreshToken = currentUser?.refresh_token ?? null;

    try {
      await prepareLogoutSession({
        accessToken,
        idToken,
        refreshToken,
      });
    } catch (error) {
      console.warn('No se pudo preparar el logout en el backend:', error);
    }

    await clearLocalOidcState();

    const postLogoutRedirectUri = `${window.location.origin}/`;

    notifyLogout({
      message: 'Se cerro la sesion local y se redirigira al login de Keycloak.',
    });

    try {
      return await auth.signoutRedirect({
        id_token_hint: idToken,
        post_logout_redirect_uri: postLogoutRedirectUri,
      });
    } catch (error) {
      console.warn('No se pudo cerrar la sesion global en Keycloak, forzando retorno al login:', error);
      window.location.assign(postLogoutRedirectUri);
      return null;
    }
  })();

  try {
    return await logoutRedirectPromise;
  } finally {
    logoutRedirectPromise = null;
  }
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
