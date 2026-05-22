/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import apiClient from '../api/axiosConfig';
import { auth, extractRolesFromToken } from '../utils/auth';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [backendProfile, setBackendProfile] = useState(null);
  const [backendRoles, setBackendRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;

    const loadBackendIdentity = async (currentUser) => {
      if (!currentUser?.access_token) {
        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
        }
        return;
      }

      try {
        const response = await apiClient.get('/usuarios/me');
        const payload = response?.data;
        const backendUser = payload?.data ?? null;
        const backendRoleList = backendUser?.rol
          ? backendUser.rol.split(',').map((role) => role.trim()).filter(Boolean)
          : [];

        if (mounted) {
          setBackendProfile(backendUser);
          setBackendRoles(backendRoleList);
        }
      } catch (profileError) {
        if (mounted) {
          setBackendProfile(null);
          setBackendRoles([]);
        }

        console.warn('No fue posible obtener el perfil validado por backend:', profileError);
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
      setLoading(false);
    };

    const handleSilentRenewError = (renewError) => {
      console.error('Error renovando token:', renewError);
      setError(renewError);
    };

    const handleAccessTokenExpired = () => {
      auth.signinRedirect().catch((redirectError) => {
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

  const value = {
    isAuthenticated,
    loading,
    error,
    user,
    backendProfile,
    roles,
    accessToken: user?.access_token ?? null,
    login: () => auth.signinRedirect(),
    logout: async () => {
      try {
        await auth.removeUser();
        await auth.clearStaleState();
      } catch (clearError) {
        console.warn('No se pudo limpiar el estado OIDC antes del logout:', clearError);
      }

      return auth.signoutRedirect();
    },
    hasRole: (role) => roles.includes(role),
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


