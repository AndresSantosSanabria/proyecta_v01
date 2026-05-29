import { Navigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';

const AnalyticsRoute = () => {
  const { isAdminLocal, transversal, hasPermission, hasRole } = useAuthContext();
  const canViewAnalytics = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasRole('GESTOR_TIC')
    || hasPermission('PROYECTO:VER');

  if (!canViewAnalytics) {
    return <Navigate to="/access-denied" replace />;
  }

  return <Outlet />;
};

export default AnalyticsRoute;
