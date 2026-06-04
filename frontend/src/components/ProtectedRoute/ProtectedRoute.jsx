import { useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';
import { startLoginRedirect } from '../../utils/auth';

const LoadingState = ({ title, subtitle }) => (
  <div style={{
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    backgroundColor: '#f5f5f5',
    flexDirection: 'column',
    textAlign: 'center',
    padding: '24px',
  }}>
    <h2 style={{ marginBottom: '8px' }}>{title}</h2>
    <p>{subtitle}</p>
  </div>
);

const ProtectedRoute = () => {
  const { assignedProjects, isAuthenticated, loading, error, hasRole, permissions, logout, isAdminLocal, transversal } = useAuthContext();
  const location = useLocation();
  const loginTriggeredRef = useRef(false);

  useEffect(() => {
    if (!loading && !isAuthenticated && !loginTriggeredRef.current) {
      loginTriggeredRef.current = true;
      startLoginRedirect().catch((authError) => {
        loginTriggeredRef.current = false;
        console.error('Error iniciando login:', authError);
      });
    }
  }, [loading, isAuthenticated]);

  if (loading) {
    return (
      <LoadingState
        title="Verificando identidad con Keycloak..."
        subtitle="Por favor espera..."
      />
    );
  }

  if (error) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        backgroundColor: '#f5f5f5',
        flexDirection: 'column',
        textAlign: 'center',
        padding: '24px',
      }}>
        <h2>Error de autenticacion</h2>
        <p style={{ maxWidth: '560px' }}>{error.message || 'No fue posible validar la sesion.'}</p>
        <button
          onClick={() => {
            loginTriggeredRef.current = false;
            startLoginRedirect();
          }}
          style={{
            padding: '10px 20px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            marginTop: '20px',
          }}
        >
          Volver a intentar
        </button>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoadingState title="Redirigiendo a Keycloak..." subtitle={`Ruta solicitada: ${location.pathname}`} />;
  }

  const hasBaseAccess = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasRole('APP_ACCESS')
    || hasRole('DIRECTOR_PROYECTO')
    || (Array.isArray(assignedProjects) && assignedProjects.length > 0)
    || (Array.isArray(permissions) && permissions.length > 0);

  if (!hasBaseAccess) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        backgroundColor: '#f5f5f5',
        flexDirection: 'column',
        textAlign: 'center',
        padding: '24px',
      }}>
        <h2>Acceso denegado</h2>
        <p>Tu usuario no tiene acceso base al sistema o no está marcado como administrador local.</p>
        <button
          onClick={() => {
            loginTriggeredRef.current = false;
            logout();
          }}
          style={{
            padding: '10px 20px',
            backgroundColor: '#333',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            marginTop: '20px',
          }}
        >
          Cerrar sesion
        </button>
      </div>
    );
  }

  return <Outlet />;
};

export default ProtectedRoute;
