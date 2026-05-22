/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { auth, extractRolesFromToken } from '../utils/auth';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;

    const syncUser = async () => {
      try {
        setLoading(true);
        const currentUser = await auth.getUser();

        if (!mounted) {
          return;
        }

        setUser(currentUser ?? null);
        setError(null);
      } catch (err) {
        if (mounted) {
          setError(err);
          setUser(null);
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    const handleUserLoaded = (loadedUser) => {
      setUser(loadedUser ?? null);
      setError(null);
      setLoading(false);
    };

    const handleUserUnloaded = () => {
      setUser(null);
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

  const roles = useMemo(() => extractRolesFromToken(user?.access_token), [user?.access_token]);
  const isAuthenticated = Boolean(user && user.access_token && !user.expired);

  const value = {
    isAuthenticated,
    loading,
    error,
    user,
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


