package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.document.DocumentoListResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResponseDTO;
import com.proyecta.api_gestion.model.enums.TipoDocumento;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentoService {
    DocumentoListResponseDTO listarDocumentos(String proyectoId);
    DocumentoUploadResponseDTO cargarDocumento(String proyectoId, TipoDocumento tipoDocumento, MultipartFile archivo);
    Resource descargarDocumento(String proyectoId, TipoDocumento tipoDocumento);
}
