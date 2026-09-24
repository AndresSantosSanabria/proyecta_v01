package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.document.DocumentoListadoResponseDTO;
import com.proyecta.api_gestion.dto.document.DocumentoPreWizardConfirmacionDTO;
import com.proyecta.api_gestion.dto.document.DocumentoPreWizardRevisionDTO;
import com.proyecta.api_gestion.dto.document.DocumentoUploadResultDTO;
import com.proyecta.api_gestion.dto.document.DocumentoVersionHistorialResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentoService {
    DocumentoListadoResponseDTO listarDocumentos(String proyectoId);
    DocumentoUploadResultDTO cargarDocumento(String proyectoId, String tipoDocumento, MultipartFile archivo, String observacion, Authentication authentication);
    Resource descargarDocumento(String proyectoId, String tipoDocumento);
    void eliminarDocumento(String proyectoId, String tipoDocumento, Authentication authentication);
    DocumentoVersionHistorialResponseDTO listarVersiones(String proyectoId, String tipoDocumento);
    Resource descargarVersion(String proyectoId, String tipoDocumento, Integer numeroVersion);
    DocumentoPreWizardRevisionDTO.Listado listarRevisionesPreWizard(String proyectoId);
    DocumentoPreWizardRevisionDTO aprobarDocumentoPreWizard(String proyectoId, String tipoDocumento, Authentication authentication);
    DocumentoPreWizardRevisionDTO devolverDocumentoPreWizard(String proyectoId, String tipoDocumento, String observaciones, Authentication authentication);
    DocumentoPreWizardConfirmacionDTO confirmarRevisionPreWizard(String proyectoId, Authentication authentication);
}
