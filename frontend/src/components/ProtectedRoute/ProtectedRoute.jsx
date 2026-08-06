import { useEffect, useRef } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { LogIn, RefreshCcw, ShieldAlert, Sparkles } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';
import { startLoginRedirect } from '../../utils/auth';

const LoadingState = ({ title, subtitle }) => (
  <div style={{
    minHeight: '100vh',
    display: 'grid',
    placeItems: 'center',
    padding: '24px',
    background: `
      radial-gradient(circle at top, rgba(14, 165, 233, 0.12), transparent 36%),
      linear-gradient(160deg, #0b1220 0%, #111827 100%)
    `,
    color: '#e2e8f0',
  }}>
    <div style={{
      width: 'min(100%, 560px)',
      background: 'rgba(15, 23, 42, 0.84)',
      border: '1px solid rgba(148, 163, 184, 0.16)',
      borderRadius: '28px',
      boxShadow: '0 28px 80px rgba(2, 6, 23, 0.45)',
      padding: '32px',
      textAlign: 'center',
      backdropFilter: 'blur(18px)',
    }}>
      <div style={{
        width: '72px',
        height: '72px',
        borderRadius: '22px',
        margin: '0 auto 18px',
        display: 'grid',
        placeItems: 'center',
        background: 'rgba(14, 165, 233, 0.12)',
        color: '#7dd3fc',
      }}>
        <RefreshCcw size={34} />
      </div>
      <h2 style={{ margin: '0 0 10px', fontSize: '1.6rem' }}>{title}</h2>
      <p style={{ margin: 0, color: '#cbd5e1', lineHeight: 1.65 }}>{subtitle}</p>
    </div>
  </div>
);

const ProtectedErrorState = ({ title, message, onRetry }) => (
  <div style={{
    minHeight: '100vh',
    display: 'grid',
    placeItems: 'center',
    padding: '24px',
    background: `
      radial-gradient(circle at top right, rgba(248, 113, 113, 0.14), transparent 34%),
      linear-gradient(160deg, #0b1220 0%, #111827 100%)
    `,
    color: '#e2e8f0',
  }}>
    <div style={{
      width: 'min(100%, 620px)',
      background: 'rgba(15, 23, 42, 0.88)',
      border: '1px solid rgba(248, 113, 113, 0.18)',
      borderRadius: '30px',
      boxShadow: '0 28px 80px rgba(2, 6, 23, 0.55)',
      padding: '36px',
      textAlign: 'center',
      backdropFilter: 'blur(18px)',
    }}>
      <div style={{
        width: '84px',
        height: '84px',
        borderRadius: '26px',
        margin: '0 auto 18px',
        display: 'grid',
        placeItems: 'center',
        background: 'rgba(248, 113, 113, 0.14)',
        color: '#fca5a5',
      }}>
        <ShieldAlert size={38} />
      </div>

      <h2 style={{ margin: '0 0 12px', fontSize: '2rem', letterSpacing: '-0.03em' }}>{title}</h2>
      <p style={{ margin: '0 auto', maxWidth: '56ch', color: '#cbd5e1', lineHeight: 1.7 }}>
        {message}
      </p>

      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
        gap: '12px',
        marginTop: '24px',
      }}>
        {[
          { label: 'Estado', value: 'Sesion no autorizada' },
          { label: 'Accion', value: 'Volver a autenticar' },
        ].map((item) => (
          <div key={item.label} style={{
            padding: '14px',
            borderRadius: '18px',
            background: 'rgba(255, 255, 255, 0.04)',
            border: '1px solid rgba(148, 163, 184, 0.14)',
          }}>
            <div style={{ color: '#94a3b8', fontSize: '0.88rem', marginBottom: '6px' }}>{item.label}</div>
            <strong style={{ color: '#f8fafc' }}>{item.value}</strong>
          </div>
        ))}
      </div>

      <div style={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'center',
        gap: '12px',
        marginTop: '28px',
      }}>
        <button
          onClick={onRetry}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '8px',
            padding: '12px 18px',
            borderRadius: '12px',
            border: 'none',
            background: 'linear-gradient(135deg, #0ea5e9, #2563eb)',
            color: '#fff',
            cursor: 'pointer',
            fontWeight: 800,
            boxShadow: '0 16px 30px rgba(37, 99, 235, 0.26)',
          }}
        >
          <LogIn size={16} />
          Iniciar sesion otra vez
        </button>
      </div>

      <p style={{
        margin: '18px 0 0',
        display: 'inline-flex',
        alignItems: 'center',
        gap: '8px',
        color: '#94a3b8',
        fontSize: '0.92rem',
      }}>
        <Sparkles size={16} />
        {message}
      </p>
    </div>
  </div>
);

const ProtectedRoute = () => {
  const {
    assignedProjects,
    isAuthenticated,
    loading,
    backendLoading,
    error,
    hasRole,
    permissions,
    isAdminLocal,
    transversal,
  } = useAuthContext();
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

  if (loading || backendLoading) {
    return (
      <LoadingState
        title="Verificando identidad con Keycloak..."
        subtitle="Estamos validando tu sesion y renovando los permisos necesarios para continuar."
      />
    );
  }

  if (error) {
    const message = error.message || 'No fue posible validar la sesion.';
    const isBackendUserIssue = message.toLowerCase().includes('backend') || message.toLowerCase().includes('inactivo');

    return (
      <ProtectedErrorState
        title={isBackendUserIssue ? 'La sesion no pudo validarse' : 'No fue posible iniciar sesion'}
        message={isBackendUserIssue
          ? 'Tu usuario se autentico correctamente, pero el backend no lo pudo resolver o ya no esta activo. Intenta volver a entrar para refrescar los datos.'
          : message}
        onRetry={() => {
          loginTriggeredRef.current = false;
          startLoginRedirect().catch((authError) => {
            console.error('Error reiniciando login:', authError);
          });
        }}
      />
    );
  }

  if (!isAuthenticated) {
    return <LoadingState title="Redirigiendo a Keycloak..." subtitle={`Ruta solicitada: ${location.pathname}`} />;
  }

  const hasBaseAccess = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasRole('DIRECTOR_PROYECTO')
    || (Array.isArray(assignedProjects) && assignedProjects.length > 0)
    || (Array.isArray(permissions) && permissions.length > 0);

  if (!hasBaseAccess) {
    return (
      <Navigate
        to="/"
        replace
        state={{
          reason: 'No se encontro un rol funcional activo en BD. Se aplico el rol visualizador sin permisos.',
        }}
      />
    );
  }

  return <Outlet />;
};

export default ProtectedRoute;
