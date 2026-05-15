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
};

export default projectService;
