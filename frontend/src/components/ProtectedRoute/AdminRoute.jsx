import { Navigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';

const AdminRoute = () => {
  const { hasPermission, transversal, isAdminLocal, hasRole } = useAuthContext();

  const hasAdminAccess = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('CONFIGURACION:VER')
    || hasPermission('SISTEMA:CONFIGURAR');

  if (!hasAdminAccess) {
    if (isAdminLocal || transversal || hasRole('ADMIN') || hasPermission('DASHBOARD:VER')) {
      return <Navigate to="/" replace />;
    }

    if (hasPermission('PROYECTO:VER')) {
      return <Navigate to="/projects" replace />;
    }

    if (hasPermission('REPORTE:VER')) {
      return <Navigate to="/reports" replace />;
    }

    if (hasPermission('ANALITICA:VER')) {
      return <Navigate to="/analytics" replace />;
    }

    return <Navigate to="/access-denied" replace />;
  }

  return <Outlet />;
};

export default AdminRoute;
