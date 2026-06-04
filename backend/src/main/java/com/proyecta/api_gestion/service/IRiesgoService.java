package com.proyecta.api_gestion.service;

import com.proyecta.api_gestion.dto.risk.RiesgoCreatedResponseDTO;
import com.proyecta.api_gestion.dto.risk.MatrizRiesgoDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoListResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoRequestDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoResponseDTO;
import com.proyecta.api_gestion.dto.risk.RiesgoSolucionAdjuntoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IRiesgoService {
    RiesgoListResponseDTO getRisksByProject(String projectId);
    List<MatrizRiesgoDTO> getRiskMatrix();
    RiesgoCreatedResponseDTO createRisk(String projectId, RiesgoRequestDTO requestDto);
    RiesgoResponseDTO updateRisk(String projectId, Integer riesgoId, RiesgoRequestDTO requestDto);
    void deleteRisk(String projectId, Integer riesgoId);
    void verificarTratamiento(String projectId, Integer riesgoId, String verificacion);
    List<RiesgoSolucionAdjuntoDTO> listarSoluciones(String projectId, Integer riesgoId);
    List<RiesgoSolucionAdjuntoDTO> agregarSoluciones(String projectId, Integer riesgoId, MultipartFile[] archivos);
    org.springframework.core.io.Resource descargarSolucion(String projectId, Integer riesgoId, Long solucionId);
}
