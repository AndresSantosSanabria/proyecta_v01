import apiClient from '../api/axiosConfig';

const documentService = {
  listarDocumentos: async (proyectoId) => {
    const { data } = await apiClient.get(`/proyectos/${proyectoId}/documentos`);
    return data;
  },

  cargarDocumento: async (proyectoId, tipoDocumento, file, onUploadProgress) => {
    const formData = new FormData();
    formData.append('archivo', file);

    const { data } = await apiClient.post(
      `/proyectos/${proyectoId}/documentos/${tipoDocumento}`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (progressEvent) => {
          if (onUploadProgress && progressEvent.total) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onUploadProgress(percentCompleted);
          }
        },
      }
    );
    return data;
  },

  descargarDocumento: async (proyectoId, tipoDocumento) => {
    const response = await apiClient.get(
      `/proyectos/${proyectoId}/documentos/${tipoDocumento}/descargar`,
      { responseType: 'blob' }
    );
    return response.data;
  },

  eliminarDocumento: async (proyectoId, tipoDocumento) => {
    const { data } = await apiClient.delete(
      `/proyectos/${proyectoId}/documentos/${tipoDocumento}`
    );
    return data;
  },
};

export default documentService;
