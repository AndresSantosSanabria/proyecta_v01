import apiClient from '../api/axiosConfig';

const normalizeProjectId = (id) => String(id || '').trim().toUpperCase();

/**
 * Servicio encargado de la gestión de proyectos.
 * Sigue el principio de Single Responsibility centrándose solo en la comunicación de red.
 */
const projectService = {
  /**
   * Obtiene la lista completa de proyectos.
   * @returns {Promise<Array>}
   */
  getAll: async (params = {}) => {
    const { data } = await apiClient.get('/proyectos', { params });
    return data;
  },

  /**
   * Obtiene todos los proyectos recorriendo la paginacion del backend.
   * @param {Object} params filtros opcionales (nombre, codigo, dependencia, estado, peti)
   * @returns {Promise<Array>}
   */
  getAllUnpaged: async (params = {}) => {
    const pageSize = 100;
    let page = 0;
    let hasNext = true;
    const allProjects = [];

    while (hasNext) {
      const response = await projectService.getAll({
        ...params,
        page,
        size: pageSize,
        sort: 'id,asc',
      });

      const payload = response?.data?.data ?? response?.data;

      // Compatibilidad: si algun ambiente devuelve arreglo plano en lugar de Page.
      if (Array.isArray(payload)) {
        allProjects.push(...payload);
        break;
      }

      const content = Array.isArray(payload?.content) ? payload.content : [];
      allProjects.push(...content);

      hasNext = Boolean(payload && payload.last === false);
      page += 1;
    }

    return allProjects;
  },

  /**
   * Obtiene un proyecto por su ID.
   * @param {string|number} id 
   * @returns {Promise<Object>}
   */
  getById: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}`);
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
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/avance`);
    return data;
  },

  /**
   * Obtiene el resumen ejecutivo del proyecto previo al cierre.
   * @param {string|number} id 
   * @returns {Promise<Object>}
   */
  getSummary: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/resumen`);
    return data;
  },

  /**
   * Cierra formalmente un proyecto (genera el acta de cierre).
   * @param {string|number} id 
   * @param {Object} closureData { resumenEjecutivo }
   * @returns {Promise<Object>}
   */
  closeProject: async (id, closureData) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(id)}/cierre`, closureData);
    return data;
  },

  uploadEvidencia: async (proyectoId, entregableId, file, fechaEntrega, onUploadProgress) => {
    const formData = new FormData();
    formData.append('evidencia', file);
    formData.append('fechaEntrega', fechaEntrega || new Date().toISOString().split('T')[0]);

    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/completar`,
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
