import { Navigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';

const PermissionRoute = ({ permissions = [] }) => {
  const { assignedProjects, isAdminLocal, transversal, hasRole, hasPermission } = useAuthContext();

  const allowedByRole = isAdminLocal || transversal || hasRole('ADMIN');
  const allowedByPermission = permissions.some((permission) => hasPermission(permission));
  const isProjectListAccess = permissions.includes('PROYECTO:VER');
  const allowedProjectDirector = isProjectListAccess
    && (hasRole('DIRECTOR_PROYECTO') || (Array.isArray(assignedProjects) && assignedProjects.length > 0));

  if (!allowedByRole && !allowedByPermission && !allowedProjectDirector) {
    return <Navigate to="/access-denied" replace />;
  }

  return <Outlet />;
};

export default PermissionRoute;
