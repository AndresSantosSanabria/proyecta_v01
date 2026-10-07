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
  // CWE-922: los tokens OIDC se guardan en sessionStorage (no localStorage)
  // para que se borren al cerrar el navegador y la ventana de ataque de XSS
  // no persista tras la sesión.
  userStore: new WebStorageStateStore({ store: globalThis.sessionStorage }),
  metadata,
});

let loginRedirectPromise = null;
let silentRenewPromise = null;
let logoutRedirectPromise = null;

/**
 * Prefijo de despliegue (Vite `base`), ej. '/apps/proyecta/public/' en producción
 * o '/' en desarrollo local.
 */
export const BASE_PATH = import.meta.env.BASE_URL;

/**
 * Quita el prefijo de despliegue de un pathname para que las rutas internas de
 * React Router (que ya resuelven contra `basename`) no lleven el prefijo dos veces.
 */
export function stripBasePath(pathname) {
  if (typeof pathname !== 'string' || pathname === '') return '/';
  if (BASE_PATH === '/' || !pathname.startsWith(BASE_PATH)) return pathname;
  const rest = pathname.slice(BASE_PATH.length);
  return rest ? `/${rest}` : '/';
}

export async function clearOidcStaleState() {
  try {
    await auth.clearStaleState();
  } catch (error) {
    console.warn('No fue posible limpiar el estado OIDC previo al login:', error);
  }
}

export async function startLoginRedirect(returnUrl) {
  if (loginRedirectPromise) {
    return loginRedirectPromise;
  }

  loginRedirectPromise = (async () => {
    await clearOidcStaleState();
    let next = stripBasePath(returnUrl || globalThis.location.pathname || '/');
    if (next === '/callback' || next.startsWith('/callback?')) {
      next = '/';
    }
    return auth.signinRedirect({ state: next });
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

    // Bajo subruta, origin + '/' llevaría a la raíz del dominio (otra app);
    // se usa el prefijo de despliegue para volver al inicio de Proyecta.
    const postLogoutRedirectUri = `${globalThis.location.origin}${BASE_PATH}`;

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
      globalThis.location.assign(postLogoutRedirectUri);
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

    const normalized = payload.replaceAll(/-/g, '+').replaceAll(/_/g, '/');
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
    const json = globalThis.atob(padded);
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
