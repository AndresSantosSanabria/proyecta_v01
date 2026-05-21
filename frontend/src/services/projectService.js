import apiClient from '../api/axiosConfig';

/**
 * Servicio encargado de la gestión de proyectos.
 * Sigue el principio de Single Responsibility centrándose solo en la comunicación de red.
 */
const projectService = {
  /**
   * Obtiene la lista completa de proyectos.
   * @returns {Promise<Array>}
   */
  getAll: async () => {
    const { data } = await apiClient.get('/proyectos');
    return data;
  },

  /**
   * Obtiene un proyecto por su ID.
   * @param {string|number} id 
   * @returns {Promise<Object>}
   */
  getById: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${id}`);
    return data;
  },

  /**
   * Crea un nuevo proyecto.
   * @param {Object} projectData 
   * @returns {Promise<Object>}
   */
  create: async (projectData) => {
    const { data } = await apiClient.post('/proyectos', projectData);
    return data;
  },

  /**
   * Obtiene el avance (KPIs) de un proyecto específico.
   * @param {string|number} id 
   * @returns {Promise<Object>}
   */
  getProgress: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${id}/avance`);
    return data;
  },

  /**
   * Obtiene el resumen ejecutivo del proyecto previo al cierre.
   * @param {string|number} id 
   * @returns {Promise<Object>}
   */
  getSummary: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${id}/resumen`);
    return data;
  },

  /**
   * Cierra formalmente un proyecto (genera el acta de cierre).
   * @param {string|number} id 
   * @param {Object} closureData { resumenEjecutivo }
   * @returns {Promise<Object>}
   */
  closeProject: async (id, closureData) => {
    const { data } = await apiClient.post(`/proyectos/${id}/cierre`, closureData);
    return data;
  },

  uploadEvidencia: async (proyectoId, entregableId, file, fechaEntrega, onUploadProgress) => {
    const formData = new FormData();
    formData.append('evidencia', file);
    formData.append('fechaEntrega', fechaEntrega || new Date().toISOString().split('T')[0]);

    const { data } = await apiClient.post(
      `/proyectos/${proyectoId}/avance/entregables/${entregableId}/completar`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onUploadProgress && progressEvent.total) {
            const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onUploadProgress(percent);
          }
        },
      }
    );
    return data;
  },
};

export default projectService;
