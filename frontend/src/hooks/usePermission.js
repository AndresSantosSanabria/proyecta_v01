import { useAuthContext } from '../context/AuthContext';

export function usePermission(permissionCode) {
  const { hasPermission, isAdminLocal, transversal, hasRole } = useAuthContext();
  if (isAdminLocal || transversal || hasRole('ADMIN') || hasRole('GESTOR_TIC')) {
    return true;
  }
  return hasPermission(permissionCode);
}

export function useProjectAccess(projectId) {
  const { assignedProjects, transversal, hasPermission, isAdminLocal } = useAuthContext();
  if (isAdminLocal || transversal || hasPermission('SISTEMA:CONFIGURAR')) {
    return true;
  }
  const normalizedProjectId = (projectId || '').toString().trim().toLowerCase();
  return assignedProjects.some((item) => (item || '').toString().trim().toLowerCase() === normalizedProjectId);
}
