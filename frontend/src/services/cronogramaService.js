import apiClient from '../api/axiosConfig';

/**
 * Servicio encargado de la gestión del cronograma del proyecto.
 */
const cronogramaService = {
  /**
   * Obtiene los datos del cronograma (fases e hitos) para su visualización.
   * @param {string|number} proyectoId - ID del proyecto.
   * @returns {Promise<Object>}
   */
  getCronograma: async (proyectoId) => {
    try {
      const { data } = await apiClient.get(`/proyectos/${proyectoId}/cronograma`);
      return data;
    } catch (error) {
      console.error(`Error fetching cronograma for project ${proyectoId}:`, error);
      throw error;
    }
  },

  /**
   * Obtiene el resumen informativo del proyecto para el panel lateral.
   * @param {string|number} proyectoId - ID del proyecto.
   * @returns {Promise<Object>}
   */
  getResumenProyecto: async (proyectoId) => {
    try {
      const { data } = await apiClient.get(`/proyectos/${proyectoId}/resumen`);
      return data;
    } catch (error) {
      console.error(`Error fetching resumen for project ${proyectoId}:`, error);
      throw error;
    }
  },

  /**
   * Sube un archivo PDF de cronograma para el proyecto.
   * @param {string|number} proyectoId - ID del proyecto.
   * @param {File} file - El archivo PDF a subir.
   * @returns {Promise<Object>}
   */
  uploadCronograma: async (proyectoId, file) => {
    try {
      const formData = new FormData();
      formData.append('archivo', file);

      const { data } = await apiClient.post(`/proyectos/${proyectoId}/cronograma`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      return data;
    } catch (error) {
      console.error(`Error uploading cronograma for project ${proyectoId}:`, error);
      throw error;
    }
  },

  /**
   * Descarga el archivo de cronograma del proyecto.
   * @param {string|number} proyectoId - ID del proyecto.
   * @returns {Promise<Blob>}
   */
  downloadCronograma: async (proyectoId) => {
    try {
      const response = await apiClient.get(`/proyectos/${proyectoId}/cronograma/descargar`, {
        responseType: 'blob',
      });
      return response.data;
    } catch (error) {
      console.error(`Error downloading cronograma for project ${proyectoId}:`, error);
      throw error;
    }
  }
};

export default cronogramaService;
