package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;

public interface ProjectClosureService {
    CierreProyectoResponse cerrarProyecto(String projectId, CierreProyectoRequest request);
    CierreProyectoResponse solicitarCierre(String projectId, Authentication authentication);
    Resource descargarActaCierre(String projectId);
}
