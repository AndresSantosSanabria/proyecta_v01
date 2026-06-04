import { useAuthContext } from '../context/AuthContext';

export function usePermission(permissionCode) {
  const { hasPermission, isAdminLocal, transversal, hasRole } = useAuthContext();
  if (isAdminLocal || transversal || hasRole('ADMIN')) {
    return true;
  }
  return hasPermission(permissionCode);
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
  const normalizedProjectId = (projectId || '').toString().trim().toLowerCase();
  return assignedProjects.some((item) => {
    const code = typeof item === 'object' && item !== null
      ? item.codigo || item.id || item.proyectoId || item.proyecto_id
      : item;
    return (code || '').toString().trim().toLowerCase() === normalizedProjectId;
  });
}
