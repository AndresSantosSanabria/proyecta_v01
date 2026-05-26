import { Outlet } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { useAuthContext } from '../../context/AuthContext';

const AdminDenied = () => (
  <div style={{
    minHeight: '100vh',
    display: 'grid',
    placeItems: 'center',
    padding: '32px',
    background: 'radial-gradient(circle at top, rgba(79, 70, 229, 0.12), transparent 40%), var(--bg-main)',
  }}>
    <div style={{
      maxWidth: '560px',
      width: '100%',
      background: 'var(--bg-card)',
      border: '1px solid var(--border-color)',
      borderRadius: '24px',
      boxShadow: 'var(--shadow-premium)',
      padding: '32px',
      textAlign: 'center',
    }}>
      <div style={{
        width: '72px',
        height: '72px',
        borderRadius: '20px',
        display: 'grid',
        placeItems: 'center',
        margin: '0 auto 18px',
        background: 'rgba(239, 68, 68, 0.12)',
        color: 'var(--danger)',
      }}>
        <ShieldAlert size={34} />
      </div>
      <h2 style={{ fontSize: '1.8rem', marginBottom: '8px' }}>Acceso restringido</h2>
      <p style={{ color: 'var(--text-muted)', marginBottom: '18px' }}>
        Esta sección solo está disponible para usuarios con rol <strong>admin</strong>.
      </p>
      <p style={{ color: 'var(--text-muted)' }}>
        Si necesitas administrar usuarios o roles, inicia sesión con la cuenta institucional autorizada.
      </p>
    </div>
  </div>
);

const AdminRoute = () => {
  const { hasPermission, transversal, isAdminLocal, hasRole } = useAuthContext();

  const hasAdminAccess = isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_TIC') || hasPermission('SISTEMA:CONFIGURAR');
  if (!hasAdminAccess) {
    return <AdminDenied />;
  }

  return <Outlet />;
};

export default AdminRoute;
