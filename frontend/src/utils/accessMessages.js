export const NO_ACCESS_MESSAGE = 'Sin accesos establecidos. Solicita al gestor de proyectos los permisos requeridos según tu rol.';

export const isForbiddenError = (error) => error?.response?.status === 403 || error?.status === 403;

export const resolveLoadErrorMessage = (error, fallback = 'No fue posible cargar el recurso.') => (
  isForbiddenError(error) ? NO_ACCESS_MESSAGE : fallback
);
