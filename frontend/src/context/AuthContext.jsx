/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import apiClient from '../api/axiosConfig';
import { auth, extractRolesFromToken, startLoginRedirect, startLogoutRedirect } from '../utils/auth';
import authzService from '../services/authzService';

export const AuthContext = createContext(null);

const normalizeRole = (role) => {
  if (!role) return '';
  const value = role.toString().trim();
  const cleaned = value.toUpperCase().startsWith('ROLE_') ? value.toUpperCase().slice(5) : value.toUpperCase();
  const aliases = {
    ADMINISTRADOR: 'ADMIN',
    GESTOR_PROYECTOS_TI: 'GESTOR_TIC',
    GESTOR_PROYECTOS: 'DIRECTOR_PROYECTO',
    ANALISTA_PROYECTOS: 'CONSULTA',
  };
  return aliases[cleaned] || cleaned;
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
        }
        return;
      }

      try {
        const [profileResponse, authzResponse] = await Promise.all([
          apiClient.get('/usuarios/me'),
          authzService.getMe(),
        ]);

        const payload = profileResponse?.data;
        const backendUser = payload?.data ?? null;
        const backendRoleList = backendUser?.rol
          ? backendUser.rol.split(',').map((role) => role.trim()).filter(Boolean)
          : [];
        const authzPayload = authzResponse?.data ?? authzResponse;
        const authzUser = authzPayload?.data ?? null;

        if (mounted) {
          setBackendProfile(backendUser);
          setBackendRoles(backendRoleList);
          setPermissions(Array.isArray(authzUser?.permisos) ? authzUser.permisos : []);
          setAssignedProjects(Array.isArray(authzUser?.proyectosAsignados) ? authzUser.proyectosAsignados : []);
          setTransversal(Boolean(authzUser?.transversal));
          setIsAdminLocal(Boolean(authzUser?.administradorLocal));
        }
      } catch (profileError) {
        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
          setPermissions([]);
          setAssignedProjects([]);
          setTransversal(false);
          setIsAdminLocal(false);
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
      const code = typeof item === 'object' && item !== null ? item.codigo || item.id : item;
      return (code || '').toString().trim().toLowerCase() === normalizedProjectId;
    });
  };

  const value = {
    isAuthenticated,
    loading,
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


