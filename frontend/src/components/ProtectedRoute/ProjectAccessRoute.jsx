import { Navigate, Outlet, useParams } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';

const ProjectAccessRoute = () => {
  const { id, codigoProyecto } = useParams();
  const { transversal, assignedProjects, hasPermission, isAdminLocal, hasRole } = useAuthContext();
  const projectId = (id || codigoProyecto || '').toLowerCase();
  const isAdminLike = isAdminLocal
    || transversal
    || hasRole('ADMIN')
    || hasPermission('SISTEMA:CONFIGURAR');

  if (!projectId) {
    return <Navigate to="/access-denied" replace />;
  }

  const isAssigned = assignedProjects.some((item) => {
    const code = typeof item === 'object' && item !== null
      ? item.codigo || item.id || item.proyectoId || item.proyecto_id
      : item;
    return (code || '').toString().trim().toLowerCase() === projectId;
  });

  if (isAdminLike || isAssigned) {
    return <Outlet />;
  }

  return <Navigate to="/access-denied" replace />;
};

export default ProjectAccessRoute;
