import { useAuthContext } from '../context/AuthContext';

export function usePermission(permissionCode) {
  const { hasPermission, isAdminLocal, transversal, hasRole } = useAuthContext();
  if (isAdminLocal || transversal || hasRole('ADMIN')) {
    return true;
  }
  const result = hasPermission(permissionCode);
  return result;
}

export function useProjectAccess(projectId) {
  const { assignedProjects, transversal, hasPermission, isAdminLocal, hasRole } = useAuthContext();
  if (
    isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('SISTEMA:CONFIGURAR')
  ) {
    return true;
  }
  if (!assignedProjects || !Array.isArray(assignedProjects)) return false;
  return assignedProjects.some((project) => project.id === projectId);
}
