import { Navigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';

const AnalyticsRoute = () => {
  const { isAdminLocal, transversal, hasPermission, hasRole } = useAuthContext();
  const canViewAnalytics = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('ANALITICA:VER');

  if (!canViewAnalytics) {
    return (
      <Navigate
        to="/"
        replace
        state={{
          reason: 'Falta el permiso ANALITICA:VER en la matriz de permisos de BD.',
        }}
      />
    );
  }

  return <Outlet />;
};

export default AnalyticsRoute;
