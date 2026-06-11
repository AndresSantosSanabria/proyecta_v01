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
   * Obtiene solo los proyectos asignados al usuario autenticado.
   * @returns {Promise<Array>}
   */
  getMyProjects: async () => {
    const { data } = await apiClient.get('/proyectos/mis-proyectos');
    const payload = data?.data ?? data;
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.items)) return payload.items;
    if (Array.isArray(payload?.proyectos)) return payload.proyectos;
    return [];
  },

  getAssignableDirectors: async () => {
    const { data } = await apiClient.get('/proyectos/directores-asignables');
    const payload = data?.data ?? data;
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.items)) return payload.items;
    if (Array.isArray(payload?.data)) return payload.data;
    return [];
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

  registerInitial: async (projectData) => {
    const { data } = await apiClient.post('/proyectos/registro-inicial', projectData);
    return data;
  },

  getCompletionStatus: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/completion-status`);
    return data;
  },

  completeInitialInfo: async (id, projectData) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(id)}/completar-informacion`, projectData);
    return data;
  },

  /**
   * Actualiza un proyecto existente.
   * @param {string|number} id
   * @param {Object} projectData
   * @returns {Promise<Object>}
   */
  update: async (id, projectData) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(id)}`, projectData);
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

  getHierarchy: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/hierarchy`);
    return data;
  },

  createFase: async (proyectoId, payload) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(proyectoId)}/fases`, payload);
    return data;
  },

  updateFase: async (proyectoId, faseId, payload) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(proyectoId)}/fases/${faseId}`, payload);
    return data;
  },

  createHito: async (proyectoId, faseId, payload) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(proyectoId)}/fases/${faseId}/hitos`, payload);
    return data;
  },

  updateHito: async (proyectoId, faseId, hitoId, payload) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(proyectoId)}/fases/${faseId}/hitos/${hitoId}`, payload);
    return data;
  },

  createEntregable: async (proyectoId, faseId, hitoId, payload) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(proyectoId)}/fases/${faseId}/hitos/${hitoId}/entregables`, payload);
    return data;
  },

  updateEntregable: async (proyectoId, entregableId, payload) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(proyectoId)}/entregables/${entregableId}`, payload);
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
   * @param {Object} closureData { resumenEjecutivo, leccionesPositivas, leccionesMejorar, recomendaciones, transferenciaActividad, transferenciaFecha, transferenciaUbicacionEvidencia, fechaCierre }
   * @returns {Promise<Object>}
   */
  closeProject: async (id, closureData) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(id)}/cierre`, closureData);
    return data;
  },

  /**
   * Descarga el acta de cierre del proyecto como PDF.
   * @param {string|number} id
   * @returns {Promise<import('axios').AxiosResponse<Blob>>}
   */
  downloadClosureActa: async (id) => {
    return apiClient.get(`/proyectos/${normalizeProjectId(id)}/cierre/descargar`, {
      responseType: 'blob',
    });
  },

  uploadEvidencia: async (proyectoId, entregableId, file, fechaEntrega, onUploadProgress) => {
    const formData = new FormData();
    formData.append('evidencia', file);
    formData.append('fechaEntrega', fechaEntrega || new Date().toISOString().split('T')[0]);

    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/evidencia`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
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

  approveEntregable: async (proyectoId, entregableId, observacion = '') => {
    const { data } = await apiClient.patch(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/aprobar`,
      null,
      {
        params: observacion ? { observacion } : undefined,
      }
    );
    return data;
  },

  rejectEntregable: async (proyectoId, entregableId, observacion) => {
    const { data } = await apiClient.patch(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/rechazar`,
      null,
      {
        params: { observacion },
      }
    );
    return data;
  },

  getEntregableVersions: async (proyectoId, entregableId) => {
    const { data } = await apiClient.get(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/versiones`
    );
    return data;
  },

  getEntregableObservations: async (proyectoId, entregableId) => {
    const { data } = await apiClient.get(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/observaciones`
    );
    return data;
  },

  markObservationCorrected: async (proyectoId, entregableId, observacionId, comentario = '') => {
    const { data } = await apiClient.put(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/observaciones/${observacionId}/subsanar`,
      { comentario }
    );
    return data;
  },

  revertEntregableVersion: async (proyectoId, entregableId, versionId, motivo) => {
    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/versiones/${versionId}/revertir`,
      { motivo }
    );
    return data;
  },
};

export default projectService;
