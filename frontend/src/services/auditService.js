import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data ?? response;

const auditService = {
  listLogs: async ({ accion, estado, usuarioId, modulo, metodoHttp, codigoEstado, search, desde, hasta, page = 0, size = 20 } = {}) => {
    const params = { page, size };
    if (accion && accion !== 'ALL') params.accion = accion;
    if (estado && estado !== 'ALL') params.estado = estado;
    if (usuarioId && usuarioId.trim()) params.usuarioId = usuarioId.trim();
    if (modulo && modulo.trim()) params.modulo = modulo.trim();
    if (metodoHttp && metodoHttp !== 'ALL') params.metodoHttp = metodoHttp;
    if (codigoEstado !== undefined && codigoEstado !== null && codigoEstado !== 'ALL') params.codigoEstado = codigoEstado;
    if (search && search.trim()) params.search = search.trim();
    if (desde) params.desde = desde;
    if (hasta) params.hasta = hasta;
    const response = await apiClient.get('/admin/auditoria/logs', { params });
    return unwrap(response);
  },

  getLogDetail: async (id) => {
    const response = await apiClient.get(`/admin/auditoria/logs/${encodeURIComponent(id)}`);
    return unwrap(response);
  },

  getStats: async () => {
    const response = await apiClient.get('/admin/auditoria/stats');
    return unwrap(response);
  },

  softDelete: async (id) => {
    const response = await apiClient.patch(`/admin/auditoria/logs/${encodeURIComponent(id)}/eliminar`);
    return unwrap(response);
  },

  restore: async (id) => {
    const response = await apiClient.patch(`/admin/auditoria/logs/${encodeURIComponent(id)}/restaurar`);
    return unwrap(response);
  },
};

export default auditService;
