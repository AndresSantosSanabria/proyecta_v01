package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoTratamientoRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IRiesgoTratamientoService {
    List<RiesgoTratamientoDTO> listarTratamientos(String projectId, Integer riesgoId);
    RiesgoTratamientoDTO crearTratamiento(String projectId, Integer riesgoId, RiesgoTratamientoRequest request, MultipartFile[] archivos);
    Resource descargarAdjunto(String projectId, Integer riesgoId, Long tratamientoId, Long adjuntoId);
}
