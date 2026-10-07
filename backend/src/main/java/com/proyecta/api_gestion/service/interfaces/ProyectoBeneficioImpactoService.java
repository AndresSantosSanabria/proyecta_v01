package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.avance.ProyectoAvanceResponseDTO;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoRequest;
import com.proyecta.api_gestion.dto.beneficioimpacto.ProyectoBeneficioImpactoResponseDTO;
import org.springframework.security.core.Authentication;

public interface ProyectoBeneficioImpactoService {
    ProyectoBeneficioImpactoResponseDTO obtener(String proyectoId, Authentication authentication);
    ProyectoBeneficioImpactoResponseDTO guardar(String proyectoId, ProyectoBeneficioImpactoRequest request, Authentication authentication);
    ProyectoBeneficioImpactoResponseDTO revisar(String proyectoId, boolean aprobado, String observaciones, Authentication authentication);
    void exigirSiCorresponde(String proyectoId, ProyectoAvanceResponseDTO avance, String actorUsername);
    void validarDiligenciado(String proyectoId);
}
