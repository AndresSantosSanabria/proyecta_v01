const AUTH_TOAST_EVENT = 'proyecta:toast';

export const emitToast = ({ title, message = '', tone = 'info', duration = 4500 }) => {
  if (typeof window === 'undefined') {
    return;
  }

  window.dispatchEvent(new CustomEvent(AUTH_TOAST_EVENT, {
    detail: {
      title,
      message,
      tone,
      duration,
    },
  }));
};

export const notifyAuth401 = ({ title = 'Sesión vencida', message } = {}) => {
  emitToast({
    title,
    message: message || 'Tu sesión expiró o el token no se pudo renovar. Vuelve a iniciar sesión para continuar.',
    tone: 'warning',
  });
};

export const notifyAuth403 = ({ title = 'Acceso denegado', message } = {}) => {
  emitToast({
    title,
    message: message || 'El token es válido, pero no tienes permisos para realizar esta acción.',
    tone: 'error',
  });
};

export const notifyLogout = ({ title = 'Sesión cerrada', message } = {}) => {
  emitToast({
    title,
    message: message || 'Tu sesión local se cerró correctamente. Ya puedes iniciar sesión otra vez.',
    tone: 'success',
  });
};
