/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import apiClient from '../api/axiosConfig';
import { auth, decodeJwtPayload, extractRolesFromToken, startLoginRedirect, startLogoutRedirect } from '../utils/auth';
import authzService from '../services/authzService';
import securityService from '../services/securityService';

export const AuthContext = createContext(null);

const roleFallbackAliases = {
  ADMINISTRADOR: 'ADMIN',
  DIRECTOR_PRO: 'DIRECTOR_PROYECTO',
  DIRECTOR_DE_PROYECTO: 'DIRECTOR_PROYECTO',
  DIRECTOR_PROYECTOS: 'DIRECTOR_PROYECTO',
  GESTOR_PRO: 'GESTOR_PROYECTOS',
  GESTOR_PROYECTO: 'GESTOR_PROYECTOS',
  GESTOR_DE_PROYECTOS: 'GESTOR_PROYECTOS',
  GESTOR_PROYECTOS_TI: 'GESTOR_TIC',
  GESTOR_DE_PROYECTOS_TI: 'GESTOR_TIC',
};

const canonicalRoleKeys = new Set([
  'ADMIN',
  'DIRECTOR_PROYECTO',
  'GESTOR_PROYECTOS',
  'GESTOR_TIC',
  'AUDITOR',
  'CONSULTA',
  'VISUALIZADOR',
]);

const normalizeRoleKey = (role) => String(role || '')
  .trim()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toUpperCase()
  .replace(/^ROLE[\s_-]+/, '')
  .replace(/[^A-Z0-9]+/g, '_')
  .replace(/^_+|_+$/g, '');

const normalizeAliasMap = (aliases = {}) => Object.entries(aliases || {}).reduce((normalized, [source, target]) => {
  const sourceKey = normalizeRoleKey(source);
  const targetKey = normalizeRoleKey(target);
  if (sourceKey && targetKey && !canonicalRoleKeys.has(sourceKey)) {
    normalized[sourceKey] = targetKey;
  }
  return normalized;
}, {});

const defaultRoleAliases = normalizeAliasMap(roleFallbackAliases);

const normalizeRole = (role, roleAliases = defaultRoleAliases) => {
  if (!role) return 'VISUALIZADOR';
  const cleaned = normalizeRoleKey(role);
  if (cleaned.includes('ADMIN')) return 'ADMIN';
  if (canonicalRoleKeys.has(cleaned)) return cleaned;
  const mapped = roleAliases[cleaned] || defaultRoleAliases[cleaned] || cleaned;
  if (canonicalRoleKeys.has(mapped)) return mapped;
  return 'VISUALIZADOR';
};

const resolveRoleList = (roles, roleAliases = defaultRoleAliases) => (
  Array.isArray(roles) ? roles.map((role) => normalizeRole(role, roleAliases)).filter(Boolean) : []
);

const isAuxiliaryTokenRole = (role) => {
  const cleaned = normalizeRoleKey(role);
  return cleaned === 'OFFLINE_ACCESS'
    || cleaned === 'UMA_AUTHORIZATION'
    || cleaned === 'APP_ACCESS'
    || cleaned.startsWith('DEFAULT_ROLES_');
};

const roleDisplayLabels = {
  ADMIN: 'Administrador',
  DIRECTOR_PROYECTO: 'Director de Proyecto',
  GESTOR_PROYECTOS: 'Gestor de Proyectos',
  GESTOR_TIC: 'Gestor TIC',
  CONSULTA: 'Consulta',
};

const formatRoleLabel = (role) => {
  if (!role || role === 'Sin Rol' || role === 'SIN_ROL') return '';
  const cleaned = normalizeRoleKey(role);
  if (!cleaned) return '';
  const mappedKey = roleFallbackAliases[cleaned] || cleaned;
  return roleDisplayLabels[mappedKey] || roleDisplayLabels[cleaned] || cleaned
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
};

const extractTokenProfile = (token, roleAliases = defaultRoleAliases) => {
  const payload = decodeJwtPayload(token);
  const normalizedRoles = resolveRoleList(extractRolesFromToken(token), roleAliases);
  const businessRoles = normalizedRoles.filter((role) => !isAuxiliaryTokenRole(role));
  const primaryRole = businessRoles[0] || '';
  const username = [
    payload?.preferred_username,
    payload?.username,
    payload?.email,
    payload?.sub,
  ]
    .map((value) => String(value || '').trim())
    .find(Boolean) || '';
  const nombre = [
    payload?.name,
    [payload?.given_name, payload?.family_name].filter(Boolean).join(' '),
    payload?.preferred_username,
    payload?.email,
  ]
    .map((value) => String(value || '').trim())
    .find(Boolean) || '';
  const correo = String(payload?.email || '').trim();
  const dependencia = [
    payload?.dependencia,
    payload?.department,
    payload?.department_name,
    payload?.departmentName,
    payload?.organizational_unit,
    payload?.organizationalUnit,
    payload?.ou,
    payload?.unit,
    payload?.area,
    payload?.division,
  ]
    .map((value) => String(value || '').trim())
    .find(Boolean) || '';

  return {
    payload,
    username,
    nombre,
    correo,
    dependencia,
    rolCodigo: primaryRole,
    rolNombre: formatRoleLabel(primaryRole),
    roles: businessRoles,
    realmRoles: payload?.realm_access?.roles ?? [],
    resourceRoles: payload?.resource_access ?? {},
  };
};

const buildUserSyncPayload = (tokenProfile) => ({
  username: tokenProfile.username,
  nombre: tokenProfile.nombre,
  correo: tokenProfile.correo,
  dependencia: tokenProfile.dependencia,
  rol: tokenProfile.rolCodigo,
  rolCodigo: tokenProfile.rolCodigo,
  rolNombre: tokenProfile.rolNombre,
  roles: tokenProfile.roles,
  activo: true,
  preferred_username: tokenProfile.payload?.preferred_username ?? '',
  sub: tokenProfile.payload?.sub ?? '',
  given_name: tokenProfile.payload?.given_name ?? '',
  family_name: tokenProfile.payload?.family_name ?? '',
  realm_roles: tokenProfile.realmRoles,
  resource_roles: tokenProfile.resourceRoles,
});

const shouldSyncBackendUser = (backendUser, tokenProfile, roleAliases = defaultRoleAliases) => {
  if (!tokenProfile?.username || !tokenProfile?.rolCodigo) {
    return false;
  }

  if (!backendUser) {
    return true;
  }

  const backendRoleCode = normalizeRole(
    backendUser?.rolCodigo || backendUser?.rol || backendUser?.role || backendUser?.roles || '',
    roleAliases
  );
  const backendUsername = String(backendUser?.username || '').trim().toLowerCase();
  const backendName = String(backendUser?.nombre || '').trim().toLowerCase();
  const backendEmail = String(backendUser?.correo || '').trim().toLowerCase();
  const backendDependency = String(backendUser?.dependencia || '').trim().toLowerCase();
  const tokenUsername = String(tokenProfile.username || '').trim().toLowerCase();
  const tokenName = String(tokenProfile.nombre || '').trim().toLowerCase();
  const tokenEmail = String(tokenProfile.correo || '').trim().toLowerCase();
  const tokenDependency = String(tokenProfile.dependencia || '').trim().toLowerCase();

  return !backendRoleCode
    || backendRoleCode !== tokenProfile.rolCodigo
    || (tokenUsername && backendUsername !== tokenUsername)
    || (tokenName && backendName !== tokenName)
    || (tokenEmail && backendEmail !== tokenEmail)
    || (tokenDependency && backendDependency !== tokenDependency);
};

const syncBackendUserFromToken = async (currentUser, roleAliases = defaultRoleAliases) => {
  try {
    // Al llamar a /authz/me, el backend hace un upsert automático.
    const response = await authzService.getMe();
    return response?.data ?? response ?? null;
  } catch (syncError) {
    console.warn('No fue posible sincronizar (upsert) el usuario autenticado:', syncError);
    return null;
  }
};

const isValidRole = (r) => {
  if (!r) return false;
  const str = String(r).trim().toUpperCase();
  return str !== '' && str !== 'SIN ROL' && str !== 'SIN_ROL' && str !== 'UNDEFINED' && str !== 'NULL';
};

const extractRoleList = (backendUser, authzUser) => {
  const rawList = [];

  const addCandidate = (candidate) => {
    if (!candidate) return;
    if (Array.isArray(candidate)) {
      candidate.forEach((r) => {
        const val = typeof r === 'object' && r !== null ? (r.codigo || r.nombre) : r;
        if (isValidRole(val)) rawList.push(String(val).trim());
      });
    } else if (typeof candidate === 'string' && isValidRole(candidate)) {
      candidate.split(',').forEach((r) => {
        if (isValidRole(r)) rawList.push(r.trim());
      });
    }
  };

  addCandidate(backendUser?.rolCodigo);
  addCandidate(backendUser?.rol_codigo);
  addCandidate(backendUser?.rol);
  addCandidate(backendUser?.rolNombre);
  addCandidate(backendUser?.rol_nombre);
  addCandidate(backendUser?.role);
  addCandidate(backendUser?.roles);
  addCandidate(authzUser?.roles);
  addCandidate(authzUser?.role);
  addCandidate(authzUser?.rol);

  const functionalRoles = rawList.filter((r) => normalizeRoleKey(r) !== 'VISUALIZADOR');
  if (functionalRoles.length > 0) {
    return functionalRoles;
  }

  return rawList;
};

const extractAssignedProjects = (authzUser) => {
  const rawProjects = authzUser?.proyectosAsignados
    || authzUser?.proyectos_asignados
    || authzUser?.proyectos
    || authzUser?.projects
    || [];

  return Array.isArray(rawProjects) ? rawProjects : [];
};

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [backendProfile, setBackendProfile] = useState(null);
  const [backendRoles, setBackendRoles] = useState([]);
  const [permissions, setPermissions] = useState([]);
  const [assignedProjects, setAssignedProjects] = useState([]);
  const [roleAliases, setRoleAliases] = useState(defaultRoleAliases);
  const [transversal, setTransversal] = useState(false);
  const [isAdminLocal, setIsAdminLocal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [backendLoading, setBackendLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;

    const readRoleAliases = async () => {
      try {
        const response = await authzService.getRoleAliases();
        const payload = response?.data ?? response ?? {};
        return normalizeAliasMap(payload?.data ?? payload);
      } catch (aliasError) {
        console.warn('No fue posible obtener alias de roles desde backend. Se usaran alias base.', aliasError);
        return defaultRoleAliases;
      }
    };

    const readBackendIdentity = async () => {
      const profileResponse = await apiClient.get('/usuarios/me');
      const backendUser = profileResponse?.data?.data ?? null;

      let authzUser = null;
      try {
        const authzResponse = await authzService.getMe();
        const authzPayload = authzResponse?.data ?? authzResponse ?? null;
        authzUser = authzPayload?.data ?? authzPayload;
      } catch (authzError) {
        console.warn('No fue posible obtener la autorizacion del usuario autenticado:', authzError);
      }

      return { backendUser, authzUser };
    };

    const applyBackendIdentity = (backendUser, authzUser) => {
      if (!mounted) {
        return;
      }

      setBackendProfile(backendUser);
      setBackendRoles(extractRoleList(backendUser, authzUser));
      setPermissions(Array.isArray(authzUser?.permisos) ? authzUser.permisos : []);
      setAssignedProjects(extractAssignedProjects(authzUser));
      setTransversal(Boolean(authzUser?.transversal));
      setIsAdminLocal(Boolean(authzUser?.administradorLocal));
    };

    const loadBackendIdentity = async (currentUser) => {
      if (!currentUser?.access_token) {
        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
          setPermissions([]);
          setAssignedProjects([]);
          setRoleAliases(defaultRoleAliases);
          setTransversal(false);
          setIsAdminLocal(false);
          setBackendLoading(false);
        }
        return;
      }

      if (mounted) setBackendLoading(true);
      let activeRoleAliases = defaultRoleAliases;

      try {
        activeRoleAliases = await readRoleAliases();
        if (mounted) {
          setRoleAliases(activeRoleAliases);
        }

        const tokenProfile = extractTokenProfile(currentUser?.access_token, activeRoleAliases);
        const initialIdentity = await readBackendIdentity();
        applyBackendIdentity(initialIdentity.backendUser, initialIdentity.authzUser);

        if (shouldSyncBackendUser(initialIdentity.backendUser, tokenProfile, activeRoleAliases)) {
          await syncBackendUserFromToken(currentUser, activeRoleAliases);
          const refreshedIdentity = await readBackendIdentity();
          applyBackendIdentity(refreshedIdentity.backendUser, refreshedIdentity.authzUser);
        }
      } catch (profileError) {
        const isForbidden = profileError?.response?.status === 403;

        if (isForbidden) {
          await syncBackendUserFromToken(currentUser, activeRoleAliases);

          try {
            const refreshedIdentity = await readBackendIdentity();
            applyBackendIdentity(refreshedIdentity.backendUser, refreshedIdentity.authzUser);
            return;
          } catch (retryError) {
            console.warn('No fue posible sincronizar el usuario autenticado tras el reintento:', retryError);
          }
        }

        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
          setPermissions([]);
          setAssignedProjects([]);
          setRoleAliases(defaultRoleAliases);
          setTransversal(false);
          setIsAdminLocal(false);
        }

        console.warn('No fue posible obtener el perfil validado por backend:', profileError);

        if (mounted && isForbidden) {
          setError(new Error('El usuario autenticado no existe o esta inactivo en el backend.'));
          startLogoutRedirect().catch((logoutError) => {
            console.error('No se pudo cerrar la sesion tras la validacion local:', logoutError);
          });
        }
      } finally {
        if (mounted) {
          setBackendLoading(false);
        }
      }
    };

    const syncUser = async () => {
      try {
        setLoading(true);
        const currentUser = await auth.getUser();

        if (!mounted) {
          return;
        }

        setUser(currentUser ?? null);
        setError(null);
        await loadBackendIdentity(currentUser);
      } catch (err) {
        if (mounted) {
          setError(err);
          setUser(null);
          setBackendProfile(null);
          setBackendRoles([]);
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    const handleUserLoaded = async (loadedUser) => {
      setUser(loadedUser ?? null);
      setError(null);
      await loadBackendIdentity(loadedUser);
    };

    const handleUserUnloaded = () => {
      setUser(null);
      setBackendProfile(null);
      setBackendRoles([]);
      setPermissions([]);
      setAssignedProjects([]);
      setRoleAliases(defaultRoleAliases);
      setTransversal(false);
      setIsAdminLocal(false);
      setLoading(false);
      setBackendLoading(false);
    };

    const handleSilentRenewError = (renewError) => {
      console.error('Error renovando token:', renewError);
      setError(renewError);
    };

    const handleAccessTokenExpired = () => {
      startLoginRedirect().catch((redirectError) => {
        console.error('No se pudo redirigir al login tras expirar el token:', redirectError);
      });
    };

    syncUser();
    auth.events.addUserLoaded(handleUserLoaded);
    auth.events.addUserUnloaded(handleUserUnloaded);
    auth.events.addSilentRenewError(handleSilentRenewError);
    auth.events.addAccessTokenExpired(handleAccessTokenExpired);

    return () => {
      mounted = false;
      auth.events.removeUserLoaded(handleUserLoaded);
      auth.events.removeUserUnloaded(handleUserUnloaded);
      auth.events.removeSilentRenewError(handleSilentRenewError);
      auth.events.removeAccessTokenExpired(handleAccessTokenExpired);
    };
  }, []);

  const tokenRoles = useMemo(() => extractRolesFromToken(user?.access_token), [user?.access_token]);
  const resolvedTokenRoles = useMemo(() => resolveRoleList(tokenRoles, roleAliases), [tokenRoles, roleAliases]);
  const resolvedBackendRoles = useMemo(() => resolveRoleList(backendRoles, roleAliases), [backendRoles, roleAliases]);
  const businessTokenRoles = useMemo(
    () => resolvedTokenRoles.filter((role) => !isAuxiliaryTokenRole(role)),
    [resolvedTokenRoles]
  );
  const roles = useMemo(
    () => Array.from(new Set([...resolvedBackendRoles, ...resolvedTokenRoles])),
    [resolvedTokenRoles, resolvedBackendRoles]
  );
  const primaryRole = useMemo(
    () => resolvedBackendRoles[0] || businessTokenRoles[0] || resolvedTokenRoles[0] || '',
    [resolvedBackendRoles, businessTokenRoles, resolvedTokenRoles]
  );
  const isAuthenticated = Boolean(user && user.access_token && !user.expired);
  const can = (permissionCode) => permissions.includes(permissionCode);
  const isProjectAssigned = (projectId) => {
    const normalizedProjectId = (projectId || '').toString().trim().toLowerCase();
    return assignedProjects.some((item) => {
      const code = typeof item === 'object' && item !== null
        ? item.codigo || item.id || item.proyectoId || item.proyecto_id
        : item;
      return (code || '').toString().trim().toLowerCase() === normalizedProjectId;
    });
  };

  const value = {
    isAuthenticated,
    loading,
    backendLoading,
    error,
    user,
    backendProfile,
    roles,
    tokenRoles: resolvedTokenRoles,
    businessTokenRoles,
    primaryRole,
    formatRoleLabel,
    backendProfile,
    roles,
    tokenRoles: resolvedTokenRoles,
    businessTokenRoles,
    primaryRole,
    permissions,
    assignedProjects,
    roleAliases,
    transversal,
    isAdminLocal,
    accessToken: user?.access_token ?? null,
    login: () => startLoginRedirect(),
    logout: () => startLogoutRedirect(),
    hasRole: (role) => {
      const expected = normalizeRole(role, roleAliases);
      return roles.some((current) => normalizeRole(current, roleAliases) === expected);
    },
    hasPermission: can,
    isProjectAssigned,
    getUser: () => auth.getUser(),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthContext() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuthContext debe usarse dentro de AuthProvider');
  }
  return context;
}
