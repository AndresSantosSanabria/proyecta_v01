const normalizePermissionCode = (value) => String(value || '')
  .trim()
  .toUpperCase()
  .replace(/\s+/g, '_')
  .replace(/^_+|_+$/g, '');

const PROJECT_PERMISSION_PREFIXES = [
  'PROYECTO:',
  'AVANCE:',
  'ENTREGABLE:',
  'EVIDENCIA:',
  'DOCUMENTO:',
  'CRONOGRAMA:',
  'BENEFICIO_IMPACTO:',
  'CIERRE:',
];

const ADMIN_PERMISSION_PREFIXES = [
  'CONFIGURACION:',
  'SISTEMA:',
];

export const normalizePermissions = (permissions = []) => (
  Array.isArray(permissions)
    ? permissions
        .filter(Boolean)
        .map((permission) => normalizePermissionCode(permission))
        .filter(Boolean)
    : []
);

export const hasAnyPermission = (permissions = [], candidates = []) => {
  const normalizedPermissions = normalizePermissions(permissions);
  return candidates.some((candidate) => normalizedPermissions.includes(normalizePermissionCode(candidate)));
};

export const hasPermissionPrefix = (permissions = [], prefixes = []) => {
  const normalizedPermissions = normalizePermissions(permissions);
  return normalizedPermissions.some((permission) => prefixes.some((prefix) => permission.startsWith(prefix)));
};

export const hasProjectScopePermission = (permissions = []) => hasPermissionPrefix(permissions, PROJECT_PERMISSION_PREFIXES);

export const hasAdminScopePermission = (permissions = []) => hasPermissionPrefix(permissions, ADMIN_PERMISSION_PREFIXES);

export const isPermissionGranted = (permissions = [], permissionCode) => (
  hasAnyPermission(permissions, [permissionCode])
);
