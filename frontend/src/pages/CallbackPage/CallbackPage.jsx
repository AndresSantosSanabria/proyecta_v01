import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, clearOidcStaleState } from '../../utils/auth';

const CALLBACK_TIMEOUT_MS = 12000;

const CallbackPage = () => {
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const handledRef = useRef(false);

  useEffect(() => {
    // StrictMode monta/desmonta el efecto en dev: no cancelar el progreso
    // en el cleanup ni el segundo run volvera a salir por handledRef y
    // el usuario se quedara eternamente en "Iniciando sesion segura...".
    if (handledRef.current) {
      return undefined;
    }
    handledRef.current = true;

    const safeReturnUrl = (raw) => {
      const value = typeof raw === 'string' ? raw.trim() : '';
      if (!value || value === '/callback' || value.startsWith('/callback?')) {
        return '/';
      }
      return value;
    };

    const go = (returnUrl) => {
      navigate(safeReturnUrl(returnUrl), { replace: true });
    };

    const clearTimer = () => {
      if (timeoutId) {
        window.clearTimeout(timeoutId);
        timeoutId = null;
      }
    };

    const params = new URLSearchParams(window.location.search);
    const hasOidcResponse =
      params.has('code') ||
      params.has('id_token') ||
      params.has('error') ||
      params.has('error_description');

    // Sin respuesta OIDC en la URL (refresh o acceso directo a /callback):
    // no llamar signinRedirectCallback (puede colgarse sin code/state).
    if (!hasOidcResponse) {
      auth
        .getUser()
        .then((user) => go(user?.state))
        .catch(() => go('/'));
      return undefined;
    }

    let timeoutId = window.setTimeout(() => {
      console.warn('[CallbackPage] signinRedirectCallback excedio el timeout');
      clearTimer();
      setError('La autenticación tardó demasiado. Regresando al inicio...');
      clearOidcStaleState().finally(() => go('/'));
    }, CALLBACK_TIMEOUT_MS);

    auth
      .signinRedirectCallback()
      .then((user) => {
        clearTimer();
        go(user?.state);
      })
      .catch((err) => {
        console.error('Error procesando callback de OIDC:', err);
        clearTimer();
        setError('Falló la autenticación. Regresando al inicio...');
        clearOidcStaleState().finally(() => go('/'));
      });

    return undefined;
  }, [navigate]);

  return (
    <div
      className="container"
      style={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        width: '100vw',
      }}
    >
      {error ? (
        <h2 style={{ color: 'red' }}>{error}</h2>
      ) : (
        <h2>Iniciando sesion segura...</h2>
      )}
    </div>
  );
};

export default CallbackPage;
