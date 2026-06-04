/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import apiClient from '../api/axiosConfig';
import { auth, extractRolesFromToken, startLoginRedirect, startLogoutRedirect } from '../utils/auth';
import authzService from '../services/authzService';
import { appConfig } from '../config/env';

export const AuthContext = createContext(null);

const roleFallbackAliases = {
  DIRECTOR_DE_PROYECTO: 'DIRECTOR_PROYECTO',
  DIRECTOR_PROYECTOS: 'DIRECTOR_PROYECTO',
  GESTOR_DE_PROYECTOS: 'DIRECTOR_PROYECTO',
};

const normalizeRoleKey = (role) => String(role || '')
  .trim()
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toUpperCase()
  .replace(/^ROLE[\s_-]+/, '')
  .replace(/[^A-Z0-9]+/g, '_')
  .replace(/^_+|_+$/g, '');

const normalizeRole = (role) => {
  if (!role) return '';
  const cleaned = normalizeRoleKey(role);
  const aliases = appConfig.roleAliases;
  return aliases[cleaned] || roleFallbackAliases[cleaned] || cleaned;
};

const extractRoleList = (backendUser) => {
  const rawRoles = backendUser?.roles
    || backendUser?.role
    || backendUser?.rol
    || backendUser?.rolCodigo
    || backendUser?.rolNombre
    || [];

  if (Array.isArray(rawRoles)) {
    return rawRoles
      .flatMap((role) => (typeof role === 'object' && role !== null ? [role.codigo, role.nombre] : role))
      .filter(Boolean)
      .map((role) => role.toString().trim())
      .filter(Boolean);
  }

  return rawRoles
    .toString()
    .split(',')
    .map((role) => role.trim())
    .filter(Boolean);
};

const extractAssignedProjects = (authzUser) => {
  const rawProjects = authzUser?.proyectosAsignados
    || authzUser?.proyectos_asignados
    || authzUser?.projectsAssigned
    || authzUser?.assignedProjects
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
  const [transversal, setTransversal] = useState(false);
  const [isAdminLocal, setIsAdminLocal] = useState(false);
  const [loading, setLoading] = useState(true);
  const [backendLoading, setBackendLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;

    const loadBackendIdentity = async (currentUser) => {
      if (!currentUser?.access_token) {
        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
          setPermissions([]);
          setAssignedProjects([]);
          setTransversal(false);
          setIsAdminLocal(false);
          setBackendLoading(false);
        }
        return;
      }

      if (mounted) setBackendLoading(true);

      try {
        const [profileResponse, authzResponse] = await Promise.all([
          apiClient.get('/usuarios/me'),
          authzService.getMe(),
        ]);

        const payload = profileResponse?.data;
        const backendUser = payload?.data ?? null;
        const backendRoleList = extractRoleList(backendUser);
        const authzPayload = authzResponse?.data ?? authzResponse ?? null;
        const authzUser = authzPayload?.data ?? authzPayload;

        if (mounted) {
          setBackendProfile(backendUser);
          setBackendRoles(backendRoleList);
          setPermissions(Array.isArray(authzUser?.permisos) ? authzUser.permisos : []);
          setAssignedProjects(extractAssignedProjects(authzUser));
          setTransversal(Boolean(authzUser?.transversal));
          setIsAdminLocal(Boolean(authzUser?.administradorLocal));
          setBackendLoading(false);
        }
      } catch (profileError) {
        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
          setPermissions([]);
          setAssignedProjects([]);
          setTransversal(false);
          setIsAdminLocal(false);
          setBackendLoading(false);
        }

        console.warn('No fue posible obtener el perfil validado por backend:', profileError);

        if (mounted && profileError?.response?.status === 403) {
          setError(new Error('El usuario autenticado no existe o está inactivo en el backend.'));
          startLogoutRedirect().catch((logoutError) => {
            console.error('No se pudo cerrar la sesión tras la validación local:', logoutError);
          });
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
      setLoading(false);
    };

    const handleUserUnloaded = () => {
      setUser(null);
      setBackendProfile(null);
      setBackendRoles([]);
      setPermissions([]);
      setAssignedProjects([]);
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
  const roles = useMemo(
    () => Array.from(new Set([...tokenRoles, ...backendRoles])),
    [tokenRoles, backendRoles]
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
    permissions,
    assignedProjects,
    transversal,
    isAdminLocal,
    accessToken: user?.access_token ?? null,
    login: () => startLoginRedirect(),
    logout: () => startLogoutRedirect(),
    hasRole: (role) => {
      const expected = normalizeRole(role);
      return roles.some((current) => normalizeRole(current) === expected);
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
