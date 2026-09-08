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
        sort: 'id,desc',
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

  getPatrocinadores: async () => {
    const { data } = await apiClient.get('/patrocinadores');
    const payload = data?.data ?? data;
    if (Array.isArray(payload)) return payload;
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

  getFurag: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/furag`);
    return data;
  },

  updateFurag: async (id, furagData) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(id)}/furag`, furagData);
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

  getSiguienteCodigo: async () => {
    const { data } = await apiClient.get('/proyectos/siguiente-codigo');
    const payload = data?.data ?? data;
    return typeof payload === 'string' ? payload : payload?.codigo ?? '';
  },

  getCompletionStatus: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/completion-status`);
    return data;
  },

  completeInitialInfo: async (id, projectData) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(id)}/completar-informacion`, projectData);
    return data;
  },

  // ==================== BORRADOR DE COMPLETITUD POR FASES ====================
  
  /**
   * Guarda el borrador de completitud del proyecto (datos parciales por fase).
   * @param {string} id - ID del proyecto
   * @param {Object} draftData - Datos del borrador (CompletitudBorradorDTO)
   * @returns {Promise<Object>}
   */
  saveCompletionDraft: async (id, draftData) => {
    const { data } = await apiClient.patch(`/proyectos/${normalizeProjectId(id)}/completitud-borrador`, draftData);
    return data;
  },

  /**
   * Obtiene el borrador de completitud guardado del proyecto.
   * @param {string} id - ID del proyecto
   * @returns {Promise<Object>}
   */
  getCompletionDraft: async (id, config = {}) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/completitud-borrador`, config);
    return data;
  },

  /**
   * Marca una fase de completitud como completada.
   * @param {string} id - ID del proyecto
   * @param {number} fase - Numero de fase (1-7)
   * @returns {Promise<Object>}
   */
  completePhase: async (id, fase) => {
    const { data } = await apiClient.patch(`/proyectos/${normalizeProjectId(id)}/completitud-fase/${fase}`);
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

  getBenefitImpact: async (id) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(id)}/beneficio-impacto`);
    return data;
  },

  saveBenefitImpact: async (id, payload) => {
    const { data } = await apiClient.put(`/proyectos/${normalizeProjectId(id)}/beneficio-impacto`, payload);
    return data;
  },

  reviewBenefitImpact: async (id, aprobado, observaciones = '') => {
    const params = { aprobado };
    if (!aprobado && observaciones) params.observaciones = observaciones;
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(id)}/beneficio-impacto/revision`, null, { params });
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

  /**
   * Solicita el cierre del proyecto (Director -> Gestor).
   * @param {string|number} id
   * @returns {Promise<Object>}
   */
  solicitarCierre: async (id, payload) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(id)}/cierre/solicitar`, payload);
    return data;
  },

  uploadTransferenciaEvidence: async (id, file) => {
    const formData = new FormData();
    formData.append('evidencia', file);
    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(id)}/cierre/evidencia-transferencia`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return data;
  },

  aprobarCierre: async (id) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(id)}/cierre/aprobar`);
    return data;
  },

  rechazarCierre: async (id, observaciones) => {
    const { data } = await apiClient.post(`/proyectos/${normalizeProjectId(id)}/cierre/rechazar`, { observaciones });
    return data;
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

  getEvidenciasByProyecto: async (proyectoId) => {
    const { data } = await apiClient.get(`/proyectos/${normalizeProjectId(proyectoId)}/evidencias`);
    const payload = data?.data ?? data;
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.evidencias)) return payload.evidencias;
    if (Array.isArray(payload?.items)) return payload.items;
    return [];
  },

  getEvidenciaDetalle: async (proyectoId, entregableId) => {
    const { data } = await apiClient.get(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/entregables/${entregableId}/evidencia`
    );
    return data?.data ?? data;
  },

  cambiarFechaEntregable: async (proyectoId, entregableId, formData) => {
    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(proyectoId)}/entregables/${entregableId}/cambiar-fecha`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return data;
  },

  obtenerHistorialFechas: async (proyectoId, entregableId) => {
    const { data } = await apiClient.get(
      `/proyectos/${normalizeProjectId(proyectoId)}/entregables/${entregableId}/historial-fechas`
    );
    return data;
  },

  cambiarDescripcionEntregable: async (proyectoId, entregableId, formData) => {
    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(proyectoId)}/entregables/${entregableId}/cambiar-descripcion`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return data;
  },

  notifyDirector: async (proyectoId) => {
    const { data } = await apiClient.post(
      `/proyectos/${normalizeProjectId(proyectoId)}/avance/alertar-director`
    );
    return data;
  },
};

export default projectService;
