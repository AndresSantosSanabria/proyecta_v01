package com.proyecta.api_gestion.controller.interfaces;

import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoRequest;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoResponseDTO;
import com.proyecta.api_gestion.dto.common.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;

public interface IProyectoBeneficioImpactoController {
    ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> obtener(String proyectoId, Authentication authentication);
    ResponseEntity<ApiResponse<ProyectoBeneficioImpactoResponseDTO>> guardar(String proyectoId, ProyectoBeneficioImpactoRequest request, Authentication authentication);
}
