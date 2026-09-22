import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { auth, clearOidcStaleState } from '../../utils/auth';

const CallbackPage = () => {
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const handledRef = useRef(false);

  useEffect(() => {
    if (handledRef.current) {
      return;
    }

    handledRef.current = true;

    auth.signinRedirectCallback()
      .then((user) => {
        const returnUrl = user?.state || '/';
        navigate(returnUrl, { replace: true });
      })
      .catch((err) => {
        console.error('Error procesando callback de OIDC:', err);
        clearOidcStaleState().finally(() => {
          setError('Falló la autenticación. Regresando al inicio...');
          navigate('/', { replace: true });
        });
      });
  }, [navigate]);

  return (
    <div className="container" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh', width: '100vw' }}>
      {error ? <h2 style={{ color: 'red' }}>{error}</h2> : <h2>Iniciando sesion segura...</h2>}
    </div>
  );
};

export default CallbackPage;
