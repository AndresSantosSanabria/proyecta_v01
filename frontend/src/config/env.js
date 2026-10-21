/**
 * Configuracion central de la aplicacion.
 *
 * Jerarquia de resolucion (de mayor a menor prioridad):
 *  1. window.__APP_CONFIG__  -> inyectado en runtime por /config.js (contenedor Docker).
 *  2. import.meta.env.VITE_*  -> horneado por Vite en build (solo util en dev local
 *     o como fallback si el contenedor no inyecto config).
 *  3. Derivada de window.location -> cuando una URL no esta configurada en ningun
 *     origen, se calcula desde el punto de montaje real en el navegador.
 *
 * Asi la misma imagen Docker sirve cualquier entorno (dev, QA, prod) sin
 * contener URLs quemadas de localhost.
 */

const runtimeConfig =
  (typeof window !== 'undefined' && window.__APP_CONFIG__) || {};

const readValue = (key) => {
  // 1) Runtime (config.js inyectado por el contenedor)
  const runtimeKey = key.replace(/^VITE_/, '');
  const runtimeValue = runtimeConfig[runtimeKey];
  if (
    runtimeValue !== undefined &&
    runtimeValue !== null &&
    runtimeValue.toString().trim() !== ''
  ) {
    return runtimeValue.toString().trim();
  }

  // 2) Build-time (import.meta.env de Vite)
  const buildValue = import.meta.env[key];
  if (
    buildValue !== undefined &&
    buildValue !== null &&
    buildValue.toString().trim() !== ''
  ) {
    return buildValue.toString().trim();
  }

  return null;
};

const requiredEnv = (key) => {
  const value = readValue(key);
  if (value === null) {
    throw new Error(`Falta configurar la variable de entorno requerida: ${key}`);
  }
  return value;
};

const optionalEnv = (key, fallback = '') => readValue(key) ?? fallback;

/**
 * Punto de montaje real de la aplicacion en el navegador.
 * Se usa como fallback cuando no hay URL configurada explicitamente.
 */
const currentOrigin =
  typeof window !== 'undefined' ? window.location.origin : '';

const basePath = import.meta.env.BASE_URL || '/';

/** Normaliza un path para que empiece con '/' y termine con '/' si no es raiz. */
const normalizeBase = (base) => {
  let b = (base || '/').toString().trim();
  if (!b.startsWith('/')) b = `/${b}`;
  if (!b.endsWith('/')) b = `${b}/`;
  return b;
};

const normalizedBase = normalizeBase(basePath);

/**
 * URL publica del backend (API).
 * Prioridad: config runtime/build > mismo origen del frontend + /api/v1.
 */
const resolveApiUrl = () => {
  const configured = readValue('VITE_API_URL');
  if (configured) return configured.replace(/\/+$/, '');
  if (currentOrigin) return `${currentOrigin}/api/v1`;
  throw new Error(
    'No se pudo resolver VITE_API_URL: no hay configuracion ni window.location disponible.'
  );
};

/**
 * Une origin + path sin producir '//' (el '//' del protocolo se preserva).
 */
const joinUrl = (origin, path) => {
  const base = origin.replace(/\/+$/, '');
  const suffix = path.startsWith('/') ? path : `/${path}`;
  return `${base}${suffix}`;
};

/**
 * URL de retorno tras login en Keycloak.
 * Prioridad: config runtime/build > origin actual + BASE_PATH + /callback.
 */
const resolveRedirectUri = () => {
  const configured = readValue('VITE_KEYCLOAK_REDIRECT_URI');
  if (configured) return configured;
  return joinUrl(currentOrigin, `${normalizedBase}callback`);
};

/**
 * URL de retorno tras logout en Keycloak.
 * Prioridad: config runtime/build > origin actual + BASE_PATH.
 */
const resolvePostLogoutRedirectUri = () => {
  const configured = readValue('VITE_KEYCLOAK_POST_LOGOUT_REDIRECT_URI');
  if (configured) return configured;
  return joinUrl(currentOrigin, normalizedBase);
};

const keycloakBaseUrl = requiredEnv('VITE_KEYCLOAK_BASE_URL').replace(
  /\/+$/,
  ''
);
const keycloakRealm = requiredEnv('VITE_KEYCLOAK_REALM');

export const appConfig = {
  apiUrl: resolveApiUrl(),
  enableAuth: optionalEnv('VITE_ENABLE_AUTH', 'true') === 'true',
  keycloak: {
    baseUrl: keycloakBaseUrl,
    realm: keycloakRealm,
    authority: `${keycloakBaseUrl}/realms/${keycloakRealm}`,
    clientId: requiredEnv('VITE_KEYCLOAK_CLIENT_ID'),
    redirectUri: resolveRedirectUri(),
    postLogoutRedirectUri: resolvePostLogoutRedirectUri(),
    responseType: optionalEnv('VITE_KEYCLOAK_RESPONSE_TYPE', 'code'),
    scope: optionalEnv('VITE_KEYCLOAK_SCOPE', 'openid profile email'),
  },
};
