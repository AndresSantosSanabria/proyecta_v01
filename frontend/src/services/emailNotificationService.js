import apiClient from '../api/axiosConfig';

const normalizeProjectId = (id) => String(id || '').trim().toUpperCase();

const emailNotificationService = {
  /**
   * Envía un correo de notificación encadenado al hilo único del proyecto.
   * @param {string} projectId
   * @param {string} mensaje Cuerpo del correo
   * @returns {Promise<{messageId: string}>}
   */
  sendThreadedEmail: async (projectId, mensaje) => {
    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(projectId)}/notificaciones/email`,
      { mensaje }
    );
    return data?.data ?? data;
  },
};

export default emailNotificationService;
