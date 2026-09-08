package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.avance.EntregableAprobadoResponseDTO;
import com.proyecta.api_gestion.dto.avance.DocumentoArchivoDTO;
import com.proyecta.api_gestion.dto.avance.DocumentoObservacionDTO;
import com.proyecta.api_gestion.dto.avance.DocumentoVersionDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface ProyectoAvanceService {
    ProyectoAvanceResponseDTO obtenerAvanceDetallado(String proyectoId);
    ProyectoSummaryDTO obtenerResumenProyecto(String proyectoId);
    EntregableAprobadoResponseDTO registrarEvidencia(String proyectoId, Integer entregableId, LocalDate fechaEntrega, MultipartFile evidencia, Authentication authentication);
    EntregableAprobadoResponseDTO aprobarEntregable(String proyectoId, Integer entregableId, Authentication authentication);
    EntregableAprobadoResponseDTO rechazarEntregable(String proyectoId, Integer entregableId, String observacion, Authentication authentication);
    List<DocumentoVersionDTO> listarVersiones(String proyectoId, Integer entregableId, Authentication authentication);
    List<DocumentoObservacionDTO> listarObservaciones(String proyectoId, Integer entregableId, Authentication authentication);
    DocumentoObservacionDTO marcarObservacionSubsanada(String proyectoId, Integer entregableId, Long observacionId, String comentario, Authentication authentication);
    EntregableAprobadoResponseDTO revertirVersion(String proyectoId, Integer entregableId, Long versionId, String motivo, Authentication authentication);
    DocumentoArchivoDTO obtenerArchivoVersion(String proyectoId, Integer entregableId, Long versionId, Authentication authentication);
    void enviarAlertaDirector(String proyectoId, Authentication authentication);
}
