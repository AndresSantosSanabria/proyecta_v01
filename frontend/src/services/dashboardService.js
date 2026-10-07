import apiClient from '../api/axiosConfig';

/**
 * Servicio encargado de las métricas y datos del dashboard.
 */
const dashboardService = {
  /**
   * Obtiene el resumen ejecutivo de los KPIs para el dashboard.
   * @returns {Promise<Object>}
   */
  getKPIs: async () => {
    try {
      const { data } = await apiClient.get('/dashboard/kpis');
      return data;
    } catch (error) {
      console.error('Error fetching dashboard KPIs:', error);
      throw error;
    }
  },

  /**
   * Obtiene el listado de proyectos recientes para la tabla del dashboard.
   * @returns {Promise<Object>}
   */
  getProjects: async () => {
    try {
      const { data } = await apiClient.get('/dashboard/avance-por-proyecto');
      return data;
    } catch (error) {
      console.error('Error fetching dashboard projects:', error);
      throw error;
    }
  }
};

export default dashboardService;
