import { Navigate, Outlet, useParams } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';
import { hasProjectScopePermission } from '../../utils/permissions';

const ProjectAccessRoute = () => {
  const { id, codigoProyecto } = useParams();
  const { transversal, assignedProjects, permissions, hasPermission, isAdminLocal, hasRole } = useAuthContext();
  const projectId = (id || codigoProyecto || '').toLowerCase();
  const isAdminLike = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('SISTEMA:CONFIGURAR');
  const normalizedAssignedProjects = Array.isArray(assignedProjects) ? assignedProjects : [];

  if (!projectId) {
    return (
      <Navigate
        to="/"
        replace
        state={{ reason: 'No se pudo resolver el proyecto solicitado.' }}
      />
    );
  }

  const isAssigned = normalizedAssignedProjects.some((item) => {
    const code = typeof item === 'object' && item !== null
      ? item.codigo || item.id || item.proyectoId || item.proyecto_id
      : item;
    return (code || '').toString().trim().toLowerCase() === projectId;
  });

  const isDirector = hasRole('DIRECTOR_PROYECTO');
  const hasProjectMatrixAccess = hasPermission('PROYECTO:VER') || hasProjectScopePermission(permissions);

  if (isAdminLike || hasProjectMatrixAccess || isAssigned || isDirector) {
    return <Outlet />;
  }

  return (
    <Navigate
      to="/"
      replace
      state={{
        reason: 'Falta acceso al proyecto en BD: PROYECTO:VER o una asignacion activa al proyecto.',
      }}
    />
  );
};

export default ProjectAccessRoute;
