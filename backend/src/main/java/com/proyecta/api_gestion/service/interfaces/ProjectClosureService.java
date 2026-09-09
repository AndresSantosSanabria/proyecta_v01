package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;

public interface ProjectClosureService {
    CierreProyectoResponse cerrarProyecto(String projectId, CierreProyectoRequest request);
    CierreProyectoResponse solicitarCierre(String projectId, CierreProyectoRequest request, Authentication authentication);
    CierreProyectoResponse aprobarCierre(String projectId, Authentication authentication);
    CierreProyectoResponse rechazarCierre(String projectId, String observaciones, Authentication authentication);
    Resource descargarActaCierre(String projectId);
    CierreProyectoResponse cierreExtraordinario(String projectId, CierreProyectoRequest request, Authentication authentication);
}
