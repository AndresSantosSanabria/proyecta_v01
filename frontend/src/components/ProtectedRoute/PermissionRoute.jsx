import { Navigate, Outlet } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';
import { hasProjectScopePermission } from '../../utils/permissions';

const PermissionRoute = ({ permissions = [] }) => {
  const { assignedProjects, isAdminLocal, transversal, hasRole, hasPermission, permissions: effectivePermissions } = useAuthContext();

  const allowedByRole = isAdminLocal || transversal || hasRole('ADMIN');
  const allowedByPermission = permissions.some((permission) => hasPermission(permission));
  const isProjectListAccess = permissions.includes('PROYECTO:VER');
  const allowedProjectDirector = isProjectListAccess
    && (hasRole('DIRECTOR_PROYECTO') || (Array.isArray(assignedProjects) && assignedProjects.length > 0));
  const allowedProjectPermission = isProjectListAccess && hasProjectScopePermission(effectivePermissions);

  if (!allowedByRole && !allowedByPermission && !allowedProjectDirector && !allowedProjectPermission) {
    return (
      <Navigate
        to="/"
        replace
        state={{
          reason: `Falta el permiso requerido en BD: ${permissions.join(', ') || 'permiso no definido'}.`,
        }}
      />
    );
  }

  return <Outlet />;
};

export default PermissionRoute;
