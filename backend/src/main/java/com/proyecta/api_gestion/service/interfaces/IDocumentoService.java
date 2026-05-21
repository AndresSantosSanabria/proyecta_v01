package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.document.DocumentoListadoResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResultDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentoService {
    DocumentoListadoResponseDTO listarDocumentos(String proyectoId);
    DocumentoUploadResultDTO cargarDocumento(String proyectoId, String tipoDocumento, MultipartFile archivo);
    Resource descargarDocumento(String proyectoId, String tipoDocumento);
    void eliminarDocumento(String proyectoId, String tipoDocumento);
}
