package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.avance.EntregableConformidadResponseDTO;
import com.proyecta.api_gestion.dto.proyecto.ProyectoSummaryDTO;
import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

public interface ProyectoAvanceService {
    ProyectoAvanceResponseDTO obtenerAvanceDetallado(String proyectoId);
    ProyectoSummaryDTO obtenerResumenProyecto(String proyectoId);
    EntregableConformidadResponseDTO actualizarConformidad(String proyectoId, Integer entregableId, Boolean conformidad, LocalDate fechaEntrega, MultipartFile evidencia);
}
