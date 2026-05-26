import { Navigate, Outlet, useParams } from 'react-router-dom';
import { useAuthContext } from '../../context/AuthContext';

const ProjectAccessRoute = () => {
  const { id, codigoProyecto } = useParams();
  const { transversal, assignedProjects, hasPermission, isAdminLocal } = useAuthContext();
  const projectId = (id || codigoProyecto || '').toLowerCase();
  const isAdminLike = isAdminLocal || transversal || hasPermission('SISTEMA:CONFIGURAR');

  if (!projectId) {
    return <Navigate to="/access-denied" replace />;
  }

  const isAssigned = assignedProjects.some((item) => {
    const code = typeof item === 'object' && item !== null ? item.codigo || item.id : item;
    return (code || '').toString().trim().toLowerCase() === projectId;
  });

  if (isAdminLike || isAssigned) {
    return <Outlet />;
  }

  console.warn('Access denied for project', projectId, 'Assigned projects:', assignedProjects);

  // Permitir acceso temporal en modo desarrollo si falla la validación
  if (import.meta.env.DEV) {
    console.warn('Permitiendo acceso en modo desarrollo por defecto.');
    return <Outlet />;
  }

  return <Navigate to="/access-denied" replace />;
};

export default ProjectAccessRoute;
