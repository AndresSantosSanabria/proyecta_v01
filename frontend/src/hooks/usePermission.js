import { useAuthContext } from '../context/AuthContext';

const DIRECTOR_BLOCKED_PERMISSIONS = new Set([
  'PROYECTO:CREAR',
  'PROYECTO:EDITAR',
  'PROYECTO:CERRAR',
  'ENTREGABLE:CREAR',
  'ENTREGABLE:EDITAR',
  'ENTREGABLE:APROBAR',
  'EVIDENCIA:EDITAR',
  'EVIDENCIA:ELIMINAR',
  'DOCUMENTO:CARGAR',
  'DOCUMENTO:EDITAR',
  'DOCUMENTO:ELIMINAR',
  'DOCUMENTO:HISTORIAL',
  'DOCUMENTO:REVERTIR',
  'CRONOGRAMA:CARGAR',
  'CRONOGRAMA:EDITAR',
  'CRONOGRAMA:ELIMINAR',
  'REPORTE:VER',
  'ANALITICA:VER',
  'CONFIGURACION:VER',
  'SISTEMA:VER',
  'SISTEMA:CREAR',
  'SISTEMA:EDITAR',
  'SISTEMA:CONFIGURAR',
]);

export function usePermission(permissionCode) {
  const { hasPermission, isAdminLocal, transversal, hasRole } = useAuthContext();
  if (isAdminLocal || transversal || hasRole('ADMIN')) {
    return true;
  }

  const isDirectorOnly = hasRole('DIRECTOR_PROYECTO')
    && !hasRole('GESTOR_TIC')
    && !hasRole('GESTOR_PROYECTOS')
    && !hasRole('GESTOR_DE_PROYECTOS');

  if (isDirectorOnly && DIRECTOR_BLOCKED_PERMISSIONS.has(permissionCode)) {
    return false;
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
