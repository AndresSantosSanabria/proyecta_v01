package com.proyecta.api_gestion.service.interfaces;

import com.proyecta.api_gestion.dto.cierre.CierreProyectoRequest;
import com.proyecta.api_gestion.dto.cierre.CierreProyectoResponse;

public interface ProjectClosureService {
    CierreProyectoResponse cerrarProyecto(String projectId, CierreProyectoRequest request);
}
