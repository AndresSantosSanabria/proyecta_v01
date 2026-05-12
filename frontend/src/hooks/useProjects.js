import { useQuery } from '@tanstack/react-query';
import projectService from '../services/projectService';

/**
 * Hook personalizado para gestionar el estado de los proyectos.
 * Implementa la Inversión de Dependencias al abstraer el servicio de los componentes.
 */
export const useProjects = () => {
  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['projects'],
    queryFn: projectService.getAll,
  });

  // Spring Boot puede devolver { content: [...] } (paginado) o un array directo
  const projects = Array.isArray(data) ? data : (data?.content ?? []);

  return {
    projects,
    isLoading,
    isError,
    error,
    refreshProjects: refetch,
  };
};

/**
 * Hook para obtener un proyecto específico.
 * @param {string|number} id 
 */
export const useProjectDetail = (id) => {
  return useQuery({
    queryKey: ['projects', id],
    queryFn: () => projectService.getById(id),
    enabled: !!id,
  });
};
