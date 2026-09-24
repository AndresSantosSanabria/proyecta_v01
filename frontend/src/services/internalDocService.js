import apiClient from '../api/axiosConfig';

const unwrap = (response) => response?.data?.data ?? response?.data ?? null;

const internalDocService = {
  listar: async ({ nombre, anio, descripcion, page, size } = {}) => {
    const params = {};
    if (nombre) params.nombre = nombre;
    if (anio) params.anio = anio;
    if (descripcion) params.descripcion = descripcion;
    if (page != null) params.page = page;
    if (size != null) params.size = size;
    const { data } = await apiClient.get('/documentos-internos', { params });
    return unwrap(data);
  },

  cargar: async ({ nombre, descripcion, fechaCreacion, file, onUploadProgress }) => {
    const formData = new FormData();
    formData.append('nombre', nombre);
    if (descripcion) formData.append('descripcion', descripcion);
    if (fechaCreacion) formData.append('fechaCreacion', fechaCreacion);
    formData.append('archivo', file);

    const { data } = await apiClient.post('/documentos-internos', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (onUploadProgress && progressEvent.total) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onUploadProgress(percentCompleted);
        }
      },
    });
    return unwrap(data);
  },

  descargar: async (id) => {
    const response = await apiClient.get(`/documentos-internos/${id}/descargar`, {
      responseType: 'blob',
    });
    return response.data;
  },

  verComoBlobUrl: async (id) => {
    const response = await apiClient.get(`/documentos-internos/${id}/ver`, {
      responseType: 'blob',
    });
    return URL.createObjectURL(response.data);
  },
};

export default internalDocService;
